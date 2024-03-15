package github.nooblong.download.bilibili;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.enums.AudioQuality;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import github.nooblong.download.utils.Constant;
import github.nooblong.download.utils.OkUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Component
public class BilibiliUtil implements InitializingBean {
    @Value("${workingDir:#{null}}")
    private String workingDir;
    private final AudioQuality expectQuality = AudioQuality._192K;// 设置此选项不会限制hires/dolby

    @Getter
    private Map<String, String> credMap = new HashMap<>();
    final OkHttpClient okHttpClient;
    private final IUserService userService;

    public BilibiliUtil(IUserService userService) {
        this.okHttpClient = new OkHttpClient.Builder().build();
        this.userService = userService;
        // 选出当前cred
        SysUser sysUser = availableBilibiliCookieUser();
        if (sysUser == null) {
            log.error("初始化BilibiliUtil失败，没有可用cookie");
        }
    }

    private Map<String, String> getCredMapByUser(SysUser user) {
        try {
            Map<String, String> result = new HashMap<>();
            Map<String, String> map = new ObjectMapper().readValue(user.getBiliCookies(), new TypeReference<>() {
            });
            result.put("sessdata", map.get("sessdata"));
            result.put("bili_jct", map.get("bili_jct"));
            if (map.get("buvid3") != null && !map.get("buvid3").isEmpty()) {
                result.put("buvid3", map.get("buvid3"));
            }
            if (map.get("buvid4") != null && !map.get("buvid4").isEmpty()) {
                result.put("buvid4", map.get("buvid4"));
            }
            if (map.get("ac_time_value") != null && !map.get("ac_time_value").isEmpty()) {
                result.put("ac_time_value", map.get("ac_time_value"));
            }
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkCredMap() {
        boolean loginRole3 = getLoginRole3();
        if (!loginRole3) {
            this.credMap = new HashMap<>();
            log.info("b站cookie失效，清除，选出新cookie");
            SysUser sysUser = availableBilibiliCookieUser();
            if (sysUser != null) {
                this.credMap = getCredMapByUser(sysUser);
                log.info("使用用户: {} 的cookie提供服务", sysUser.getUsername());
            } else {
                log.info("无可用cookie");
            }
        }
    }

    private SysUser availableBilibiliCookieUser() {
        List<SysUser> list = Db.list(SysUser.class).stream().filter(user -> StrUtil.isNotBlank(user.getBiliCookies())).toList();
        if (list.isEmpty()) {
            log.error("没有可用b站cookie");
            return null;
        }
        for (SysUser sysUser : list) {
            Map<String, String> userCredMap = getCredMapByUser(sysUser);
            boolean loginRole3 = getLoginRole3(userCredMap);
            if (loginRole3) {
                return sysUser;
            }
        }
        log.error("没有可用b站cookie");
        return null;
    }

    public Boolean needRefreshCookie() {
        JsonNode need = OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                + "/Credential/check_refresh", credMap), okHttpClient);
        if (need.get("code").asInt() != 0) {
            throw new RuntimeException("b站账号未登录");
        }
        return need.get("data").asBoolean();
    }

    public JsonNode refresh() {
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                + "/Credential/refresh", credMap), okHttpClient);
        if (response.get("code").asInt() != 0) {
            throw new RuntimeException("刷新cookie出错");
        }
        return response.get("data");
    }

    public void validate(JsonNode cookie) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> map = objectMapper.convertValue(cookie, new TypeReference<>() {
        });
        JsonNode node = OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                + "/Credential/check_valid", map), okHttpClient);
        Assert.isTrue(node.get("code").asInt() == 0 && node.get("data").asBoolean(), "验证cookie出错");
        Assert.isTrue(node.get("data").asBoolean(), "cookie刷新后无法使用");
        // save
        SysUser byId = userService.getById(Constant.adminUserId);
        ObjectNode oldCookie = ((ObjectNode) objectMapper.readTree(byId.getBiliCookies()));
        if (cookie == null) {
            log.error("validate出错");
            return;
        }
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (StrUtil.isNotEmpty(value)) {
                oldCookie.put(key, value);
            }
        }
        byId.setBiliCookies(oldCookie.toString());
        Db.updateById(byId);
    }

    public JsonNode getBestStreamUrl(BilibiliVideo bilibiliVideo) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("my_detect");
        builder.addQueryParameter("bvid", bilibiliVideo.getBvid());
        builder.addQueryParameter("cid", bilibiliVideo.getCidOrDefault());
        builder.addQueryParameter("audio_max_quality", "video.AudioQuality(" + this.expectQuality.getCode() + "):parse");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取最好的流链接失败");
        return response;
    }

    public JsonNode getUserFavoriteList(String uid) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("favorite_list").addPathSegment("get_video_favorite_list");
        builder.addQueryParameter("uid", uid);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取用户收藏列表失败");
        return response;
    }

    public Path downloadFile(BilibiliVideo video) {
        // 文件名: partName-title-aid-cid.ext
        JsonNode bestStreamUrl = getBestStreamUrl(video);
        ArrayNode arrayNode = (ArrayNode) bestStreamUrl.get("data");
        String ext = this.expectQuality.getExt();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (arrayNode.get(i).has("audio_quality")) {
                int audioQuality = arrayNode.get(i).get("audio_quality").asInt();
                ext = AudioQuality.extMap.get(audioQuality);
                break;
            }
        }
        String fileName = video.getLocalName();
        Path path = Paths.get(workingDir, Constant.OSS_BASE_FOLDER, Constant.BILI_DOWNLOAD_FOLDER);
        AtomicBoolean isDownloaded = new AtomicBoolean(false);
        try (Stream<Path> filesWalk = Files.walk(path, 1)) {
            String finalExt = ext;
            filesWalk.forEach(fw -> {
                if (path.endsWith(fw)) {
                    // skip parent directory
                } else if (Files.isDirectory(fw)) {
                    // skip
                } else if (Files.isRegularFile(fw) && fw.getFileName().toString()
                        .equals(assembleFileExt(fileName, finalExt))) {
                    log.info("文件已存在: {}", fileName);
                    isDownloaded.set(true);
                } else {
                    log.debug("getFilesInfo skipped: {} is not regular file nor directory !", path);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (isDownloaded.get()) {
            return path.resolve(assembleFileExt(fileName, ext));
        }
        return doDownload(path.toString(), fileName, bestStreamUrl, video.getBvid());
    }

    private Path doDownload(String path, String fileName, JsonNode bestStreamUrl, String bvid) {
        ArrayNode arrayNode = (ArrayNode) bestStreamUrl.get("data");
        for (int i = 0; i < arrayNode.size(); i++) {
            if (arrayNode.get(i).has("audio_quality")) {
                int audioQuality = arrayNode.get(i).get("audio_quality").asInt();
                String ext = AudioQuality.extMap.get(audioQuality);
                String url = arrayNode.get(i).get("url").asText();
                Map<String, String> headers = new HashMap<>();
                headers.put(HttpHeaders.REFERER, "https://bilibili.com/" + bvid);
                Request request = OkUtil.get(url, Collections.emptyMap(), headers);
                try (Response response = okHttpClient.newCall(request).execute()) {
                    File downloadedFile = new File(path, fileName + "." + ext);
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                    assert response.body() != null;
                    log.info("文件大小: {}M", response.body().contentLength() / 1024 / 1024);
                    sink.writeAll(response.body().source());
                    sink.close();
                    return downloadedFile.toPath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public Path downloadCover(BilibiliVideo video) throws RuntimeException {
        String url = video.getVideoInfo().get("data").get("pic").asText();
        Map<String, String> headers = new HashMap<>();
        Request request = OkUtil.get(url, Collections.emptyMap(), headers);
        try (Response response = okHttpClient.newCall(request).execute()) {
            Assert.notNull(response.body(), "下载封面失败");
            log.info("文件大小: {}K", response.body().contentLength() / 1024);
            String fileName = assembleFileExt(video.getLocalCoverName(), "jpg");
            Path path = Paths.get(workingDir, Constant.OSS_BASE_FOLDER, Constant.BILI_DOWNLOAD_FOLDER);
            File downloadedFile = new File(path.toFile(), fileName);
            BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
            sink.writeAll(response.body().source());
            sink.close();
            return downloadedFile.toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUrlBvid(String url) {
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        HttpUrl parse = HttpUrl.parse(url);
        assert parse != null;
        String topPrivateDomain = parse.topPrivateDomain();
        assert topPrivateDomain != null;
        if (topPrivateDomain.equals("b23.tv")) {
            Request request = new Request.Builder().url(url)
                    .header(HttpHeaders.USER_AGENT, OkUtil.WEAPI_AGENT).get().build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                HttpUrl httpUrl = response.request().url();
                String topPrivateDomain1 = httpUrl.topPrivateDomain();
                assert topPrivateDomain1 != null;
                Assert.isTrue(topPrivateDomain1.equals("bilibili.com"), "解析错误");
                String s = httpUrl.pathSegments().get(1);
                Assert.isTrue(s.toUpperCase().startsWith("BV"), "解析错误");
                return s;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (topPrivateDomain.equals("bilibili.com")) {
            String s = parse.pathSegments().get(1);
            Assert.isTrue(s.toUpperCase().startsWith("BV"), "解析错误");
            return s;
        } else {
            throw new RuntimeException("错误url");
        }
    }

    public IteratorCollectionTotal getCollectionVideos(String collectionId, int ps, int pn, CollectionVideoOrder collectionVideoOrder) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_videos");
        builder.addQueryParameter("ps", String.valueOf(ps));
        builder.addQueryParameter("pn", String.valueOf(pn));
        builder.addQueryParameter("id_", String.valueOf(collectionId));
        builder.addQueryParameter("type_", "ChannelSeriesType(1):parse");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取合集视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("archives"))
                .setTotalNum(response.get("data").get("page").get("total").asInt());
    }

    public IteratorCollectionTotal getFavoriteVideos(String mediaId, int page) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("favorite_list").addPathSegment("get_video_favorite_list_content");
        builder.addQueryParameter("page", String.valueOf(page));
        builder.addQueryParameter("media_id", String.valueOf(mediaId));
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取收藏夹视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("medias"))
                .setTotalNum(response.get("data").get("has_more").asBoolean() ? 1 : 0);
    }

    public static int parseStrTime(String strTime) {
        // 00:23  01:26
        // 拆分时分秒
        String[] timeParts = strTime.split(":");
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (timeParts.length == 2) {
            minutes = Integer.parseInt(timeParts[0]);
            seconds = Integer.parseInt(timeParts[1]);
        } else if (timeParts.length == 3) {
            hours = Integer.parseInt(timeParts[0]);
            minutes = Integer.parseInt(timeParts[1]);
            seconds = Integer.parseInt(timeParts[2]);
        }

        // 计算总秒数
        return (hours * 60 * 60) + (minutes * 60) + seconds;
    }

    public JsonNode getSeriesMeta(String seriesId) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "CommentResourceType(1):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public IteratorCollectionTotal getUpVideos(String upId, int ps, int pn, UserVideoOrder userVideoOrder, String keyWord) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("User").addPathSegment("get_videos");
        builder.addQueryParameter("ps", String.valueOf(ps));
        builder.addQueryParameter("pn", String.valueOf(pn));
        builder.addQueryParameter("uid", upId);
        if (StrUtil.isNotEmpty(keyWord)) {
            builder.addQueryParameter("keyword", keyWord);
        }
        builder.addQueryParameter("order", "user.VideoOrder(\"" + userVideoOrder.getValue() + "\"):parse");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取Up视频失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取Up视频失败");
        return new IteratorCollectionTotal()
                .setData((ArrayNode) response.get("data").get("list").get("vlist"))
                .setTotalNum(response.get("data").get("page").get("count").asInt());
    }

    public BilibiliVideo createByUrl(String url) {
        if (url.startsWith("http") ||
                url.startsWith("www.") ||
                url.startsWith("b23.tv") ||
                url.startsWith("bilibili")) {
            String bvid = getUrlBvid(url);
            return new BilibiliVideo().setBvid(bvid);
        } else {
            return new BilibiliVideo().setBvid(url);
        }
    }

    public void init(BilibiliVideo video) {
        Assert.notNull(video.getBvid(), "bvid为空");
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        credMap.forEach(builder::addQueryParameter);
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("get_info");
        builder.addQueryParameter("bvid", video.getBvid());
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "请求视频信息错误");
        Assert.isTrue(response.get("code").asInt() != -1, "请求视频信息错误");
        video.setVideoInfo(response);
        if (video.getHasMultiPart() && video.getCid() == null) {
            log.warn("这是一个多p视频，且没有设置cid，默认使用p1");
            video.setCid(video.getDefaultCid());
        }
    }

    private static String assembleFileExt(String fileName, String ext) {
        return fileName + "." + ext;
    }

    public static String getFileExt(String fileName) {
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex + 1);
        }
        return fileExtension;
    }

    public void afterPropertiesSet() {
        Assert.notNull(workingDir, "工作目录未提供");
    }

    public int getPartNumbers(String bvid) {
        BilibiliVideo video = createByUrl(bvid);
        init(video);
        return video.getPartNumbers();
    }

    public JsonNode getLikes() {
        return OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                + "/session/get_likes", credMap), okHttpClient);
    }

    private boolean getLoginRole3() {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                    + "/user/get_self_info", credMap), okHttpClient);
            Assert.isTrue(jsonResponse.get("data").get("vip").get("role").asInt() == 3, "不是大会员");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean getLoginRole3(Map<String, String> credMap) {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.get("http://" + Constant.FULL_BILI_API
                    + "/user/get_self_info", credMap), okHttpClient);
            Assert.isTrue(jsonResponse.get("data").get("vip").get("role").asInt() == 3, "不是大会员");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
