package github.nooblong.download.bilibili;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
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
import github.nooblong.download.entity.IteratorCollectionTotalList;
import github.nooblong.download.utils.Constant;
import github.nooblong.download.utils.OkUtil;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Component
public class BilibiliClient {
    private final AudioQuality expectQuality = AudioQuality._192K;// 设置此选项不会限制hires/dolby

    final OkHttpClient okHttpClient;
    final IUserService userService;

    public BilibiliClient(IUserService userService) {
        this.userService = userService;
        this.okHttpClient = new OkHttpClient.Builder()
                .build();
    }

    public Map<String, String> getAvailableBilibiliCookie() throws RuntimeException {
        log.info("获取可用b站cookie...");
        List<SysUser> list = Db.list(SysUser.class).stream().filter(user -> StrUtil.isNotBlank(user.getBiliCookies())).toList();
        if (list.isEmpty()) {
            throw new RuntimeException("没有可用b站cookie");
        }
        for (SysUser sysUser : list) {
            Map<String, String> userCredMap = userService.getBilibiliCookieMap(sysUser.getId());
            boolean login3 = isLogin(userCredMap);
            if (login3) {
                return userCredMap;
            }
        }
        throw new RuntimeException("没有可用b站cookie");
    }

    public JsonNode checkRefresh(Map<String, String> cred) {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                + "/Credential/check_refresh", cred), okHttpClient);
    }

    public JsonNode refresh(Map<String, String> cred) {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                + "/Credential/refresh", cred), okHttpClient);
    }

    public JsonNode getBestStreamUrl(BilibiliFullVideo bilibiliFullVideo, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("my_detect");
        builder.addQueryParameter("bvid", bilibiliFullVideo.getBvid());
        builder.addQueryParameter("cid", bilibiliFullVideo.getCid());
        builder.addQueryParameter("audio_max_quality", "video.AudioQuality(" + this.expectQuality.getCode() + "):parse");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取最好的流链接失败");
        return response;
    }

    public JsonNode getUserFavoriteList(String uid, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("favorite_list").addPathSegment("get_video_favorite_list");
        builder.addQueryParameter("uid", uid);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取用户收藏列表失败");
        return response;
    }

    public Path downloadFile(BilibiliFullVideo video, Map<String, String> cred) {
        // 文件名: partName-title-aid-cid.ext
        JsonNode bestStreamUrl = getBestStreamUrl(video, cred);
        ArrayNode arrayNode = (ArrayNode) bestStreamUrl.get("data");
        String ext = this.expectQuality.getExt();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (arrayNode.get(i).has("audio_quality")) {
                int audioQuality = arrayNode.get(i).get("audio_quality").asInt();
                ext = AudioQuality.extMap.get(audioQuality);
                break;
            }
        }
        String fileName = video.getBvid();
        Path path = Paths.get(Constant.TMP_FOLDER);
        AtomicBoolean isDownloaded = new AtomicBoolean(false);
        try (Stream<Path> filesWalk = Files.walk(path, 1)) {
            String finalExt = ext;
            filesWalk.forEach(fw -> {
                if (Files.isRegularFile(fw) && fw.getFileName().toString()
                        .equals(fileName + "." + finalExt)) {
                    log.info("文件已存在: {}", fileName);
                    isDownloaded.set(true);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (isDownloaded.get()) {
            return path.resolve(fileName + "." + ext);
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
                    log.info("音频文件大小: {}M", response.body().contentLength() / 1024 / 1024);
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

    public Path downloadCover(BilibiliFullVideo video) throws RuntimeException {
        String url = video.getVideoInfo().get("data").get("pic").asText();
        Map<String, String> headers = new HashMap<>();
        Request request = OkUtil.get(url, Collections.emptyMap(), headers);
        try (Response response = okHttpClient.newCall(request).execute()) {
            Assert.notNull(response.body(), "下载封面失败");
            log.info("封面文件大小: {}K", response.body().contentLength() / 1024);
            String fileName = video.getBvid() + "." + "jpg";
            Path path = Paths.get(Constant.TMP_FOLDER);
            File downloadedFile = new File(path.toFile(), fileName);
            BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
            sink.writeAll(response.body().source());
            sink.close();
            return downloadedFile.toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLogin(Map<String, String> credMap) {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                    + "/user/get_self_info", credMap), okHttpClient);
            Assert.isTrue(jsonResponse.get("data").get("vip").get("role").asInt() == 3, "不是大会员");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public IteratorCollectionTotal getCollectionVideos(String collectionId, int ps, int pn, CollectionVideoOrder collectionVideoOrder, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_videos");
        builder.addQueryParameter("ps", String.valueOf(ps));
        builder.addQueryParameter("pn", String.valueOf(pn));
        builder.addQueryParameter("id_", String.valueOf(collectionId));
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(1):parse");
        if (collectionVideoOrder == CollectionVideoOrder.CHANGE) {
            builder.addQueryParameter("sort", "channel_series.ChannelOrder(\"true\"):parse");
        }
        if (collectionVideoOrder == CollectionVideoOrder.DEFAULT) {
            builder.addQueryParameter("sort", "channel_series.ChannelOrder(\"false\"):parse");
        }
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取合集视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("archives"))
                .setTotalNum(response.get("data").get("page").get("total").asInt());
    }

    public IteratorCollectionTotal getOldCollectionVideos(String collectionId, int ps, int pn, CollectionVideoOrder collectionVideoOrder, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_videos");
        builder.addQueryParameter("ps", String.valueOf(ps));
        builder.addQueryParameter("pn", String.valueOf(pn));
        builder.addQueryParameter("id_", String.valueOf(collectionId));
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(0):parse");
        if (collectionVideoOrder == CollectionVideoOrder.CHANGE) {
            builder.addQueryParameter("sort", "channel_series.ChannelOrder(\"true\"):parse");
        }
        if (collectionVideoOrder == CollectionVideoOrder.DEFAULT) {
            builder.addQueryParameter("sort", "channel_series.ChannelOrder(\"false\"):parse");
        }
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取合集视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("archives"))
                .setTotalNum(response.get("data").get("page").get("total").asInt());
    }

    public IteratorCollectionTotal getFavoriteVideos(String mediaId, int page, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("favorite_list").addPathSegment("get_video_favorite_list_content");
        builder.addQueryParameter("page", String.valueOf(page));
        builder.addQueryParameter("media_id", String.valueOf(mediaId));
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取收藏夹视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("medias"))
                .setTotalNum(response.get("data").get("has_more").asBoolean() ? 1 : 0);
    }


    public JsonNode getSeriesMeta(String seriesId, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(1):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public JsonNode getOldSeriesMeta(String seriesId, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(0):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public IteratorCollectionTotal getUpVideos(String upId, int ps, int pn, UserVideoOrder userVideoOrder, String keyWord, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
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

    public JsonNode getUpChannels(String upId, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("User").addPathSegment("get_channels");
        builder.addQueryParameter("ps", "1");
        builder.addQueryParameter("pn", "20");
        builder.addQueryParameter("uid", upId);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取Up合集列表失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取Up合集失败");
        return response;
    }

    public JsonNode getSelfInfo(Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("get_self_info");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取个人信息失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取个人信息失败");
        return response;
    }

    public JsonNode getUserInfo(String upId, Map<String, String> cred) {
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("User").addPathSegment("get_user_info");
        builder.addQueryParameter("uid", upId);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取Up信息失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取Up信息失败");
        return response;
    }

    public BilibiliFullVideo init(SimpleVideoInfo video, Map<String, String> cred) {
        Assert.notNull(video.getBvid(), "bvid为空");
        Assert.isTrue(video.getBvid().toLowerCase().startsWith("bv"), "不是bv开头");
        HttpUrl.Builder builder = HttpUrl.parse(Constant.BAU).newBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("get_info");
        builder.addQueryParameter("bvid", video.getBvid());
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "请求视频信息错误");
        Assert.isTrue(response.get("code").asInt() != -1, "请求视频信息错误");
        BilibiliFullVideo bilibiliFullVideo = new BilibiliFullVideo();
        bilibiliFullVideo.setVideoInfo(response);
        bilibiliFullVideo.setSelectCid(video.getCid());
        return bilibiliFullVideo;
    }

    public BilibiliFullVideo getFullVideo(String bvid, Map<String, String> bilibiliCookie) {
        SimpleVideoInfo byUrl = createByUrl(bvid);
        return init(byUrl, bilibiliCookie);
    }

    public static String getFileExt(String fileName) {
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex + 1);
        }
        return fileExtension;
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

    public SimpleVideoInfo createByUrl(String url) {
        if (url.startsWith("http") ||
                url.startsWith("www.") ||
                url.startsWith("b23.tv") ||
                url.startsWith("bilibili")) {
            String bvid = getUrlBvid(url);
            return new SimpleVideoInfo().setBvid(bvid);
        } else if (url.toLowerCase().startsWith("bv")) {
            return new SimpleVideoInfo().setBvid(url);
        } else {
            throw new RuntimeException("未知的输入");
        }
    }

    private String getUrlBvid(String url) {
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

    public JsonNode getSpiBuvidSync() {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU + "/login/get_spi_buvid_sync"), okHttpClient);
    }

    public JsonNode updateQrcodeData() {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU + "/login_v2/QrCodeLogin/generate_qrcode"), okHttpClient);
    }

    public JsonNode loginWithKey(String key, SysUser user) {
        // 查询到成功就保存到用户cookie
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(Constant.BAU + "/login_v2/QrCodeLogin/check_state?key=" + key),
                okHttpClient);
        if (response.get("data").get("code").asInt() == 0) {
            JsonNode data = response.get("data");
            String url = data.get("url").asText();
            String refreshToken = data.get("refresh_token").asText();
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode cookieNode = objectMapper.createObjectNode();
            String[] cookieList = url.split("\\?")[1].split("&");
            for (String cookie : cookieList) {
                if (cookie.startsWith("SESSDATA")) {
                    cookie = cookie.replaceAll(",", "%2C");
                    cookie = cookie.replaceAll("/", "%2F");
                    cookieNode.put("sessdata", cookie.substring(9));
                }
                if (cookie.startsWith("bili_jct")) {
                    cookieNode.put("bili_jct", cookie.substring(9));
                }
                if (cookie.toUpperCase().startsWith("DEDEUSERID=")) {
                    cookieNode.put("dedeuserid", cookie.substring(11));
                }
            }
            cookieNode.put("ac_time_value", refreshToken);
            user.setBiliCookies(cookieNode.toString());
            userService.updateById(user);
        }

        return response;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getUpVideoListFromBilibili(String upId, int ps, int pn,
                                                                                   UserVideoOrder userVideoOrder, String keyWord,
                                                                                   Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal collectionTotal = getUpVideos(upId, ps, pn, userVideoOrder, keyWord, bilibiliCookie);
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionTotal.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(BilibiliClient.parseStrTime(jsonNode.get("length").asText()))
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("created").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionTotal.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getPartVideosFromBilibili(String bvid,
                                                                                  Map<String, String> bilibiliCookie) {
        SimpleVideoInfo video = createByUrl(bvid);
        BilibiliFullVideo bilibiliFullVideo = init(video, bilibiliCookie);
        List<SimpleVideoInfo> data = new ArrayList<>();
        bilibiliFullVideo.getPartVideos().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(bvid)
                    .setCid(jsonNode.get("cid").asText())
                    .setPartName(jsonNode.get("part").asText())
                    .setTitle(jsonNode.get("part").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(data.size());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getCollectionVideoListFromBilibili(String collectionId, int ps, int pn,
                                                                                           CollectionVideoOrder collectionVideoOrder,
                                                                                           Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal collectionVideos = null;
        try {
            collectionVideos = getCollectionVideos(collectionId, ps, pn, collectionVideoOrder, bilibiliCookie);
        } catch (Throwable e) {
            log.error("获取合集失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionVideos.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("pubdate").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionVideos.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getOldCollectionVideoListFromBilibili(String collectionId, int ps, int pn,
                                                                                              CollectionVideoOrder collectionVideoOrder,
                                                                                              Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal collectionVideos = null;
        try {
            collectionVideos = getOldCollectionVideos(collectionId, ps, pn, collectionVideoOrder, bilibiliCookie);
        } catch (Throwable e) {
            log.error("获取旧合集失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionVideos.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("pubdate").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(collectionVideos.getTotalNum());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getFavoriteVideoListFromBilibili(String favoriteId, int page, Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal favoriteVideos = getFavoriteVideos(favoriteId, page,bilibiliCookie);
        List<SimpleVideoInfo> data = new ArrayList<>();
        favoriteVideos.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(jsonNode.get("bvid").asText())
                    .setCreateTime(jsonNode.get("fav_time").asLong())
                    .setTitle(jsonNode.get("title").asText());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(favoriteVideos.getTotalNum());
        return result;
    }
}
