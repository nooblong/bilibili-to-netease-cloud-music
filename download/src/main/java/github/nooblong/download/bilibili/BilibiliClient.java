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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public boolean needRefreshCookie(Map<String, String> cred) {
        JsonNode need = OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API
                + "/Credential/check_refresh", cred), okHttpClient);
        if (need.get("code").asInt() != 0) {
            throw new RuntimeException("b站账号未登录");
        }
        return need.get("data").asBoolean();
    }

    public Map<String, String> refresh(Map<String, String> cred) {
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API
                + "/Credential/refresh", cred), okHttpClient);
        if (response.get("code").asInt() != 0) {
            throw new RuntimeException("刷新cookie出错");
        }
        JsonNode data = response.get("data");
        return new ObjectMapper().convertValue(data, new TypeReference<>() {
        });
    }

    public void validate(@Nonnull Map<String, String> cred, Long userId) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API
                + "/Credential/check_valid", cred), okHttpClient);
        Assert.isTrue(node.get("code").asInt() == 0 && node.get("data").asBoolean(), "验证cookie出错");
        Assert.isTrue(node.get("data").asBoolean(), "cookie刷新后无法使用");
        // save
        Map<String, String> oldCookie = userService.getBilibiliCookieMap(userId);
        for (String key : cred.keySet()) {
            String value = cred.get(key);
            if (StrUtil.isNotEmpty(value)) {
                oldCookie.put(key, value);
            }
        }
        userService.updateBilibiliCookieByCookieMap(userId, oldCookie);
    }

    public JsonNode getBestStreamUrl(BilibiliFullVideo bilibiliFullVideo, Map<String, String> cred) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
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
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
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
            JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API
                    + "/user/get_self_info", credMap), okHttpClient);
            Assert.isTrue(jsonResponse.get("data").get("vip").get("role").asInt() == 3, "不是大会员");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public IteratorCollectionTotal getCollectionVideos(String collectionId, int ps, int pn, CollectionVideoOrder collectionVideoOrder, Map<String, String> cred) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_videos");
        builder.addQueryParameter("ps", String.valueOf(ps));
        builder.addQueryParameter("pn", String.valueOf(pn));
        builder.addQueryParameter("id_", String.valueOf(collectionId));
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(1):parse");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取合集视频失败");
        return new IteratorCollectionTotal().setData((ArrayNode) response.get("data").get("archives"))
                .setTotalNum(response.get("data").get("page").get("total").asInt());
    }

    public IteratorCollectionTotal getFavoriteVideos(String mediaId, int page, Map<String, String> cred) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
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
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(1):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public IteratorCollectionTotal getUpVideos(String upId, int ps, int pn, UserVideoOrder userVideoOrder, String keyWord, Map<String, String> cred) {
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
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

    public BilibiliFullVideo init(SimpleVideoInfo video, Map<String, String> cred) {
        Assert.notNull(video.getBvid(), "bvid为空");
        Assert.isTrue(video.getBvid().toLowerCase().startsWith("bv"), "不是bv开头");
        HttpUrl.Builder builder = new HttpUrl.Builder().host(Constant.BILI_API_URL).port(Constant.BILI_API_PORT).scheme("http");
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
        return OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API + "/login/get_spi_buvid_sync"), okHttpClient);
    }

    public JsonNode updateQrcodeData() {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API + "/login/update_qrcode_data"), okHttpClient);
    }

    public JsonNode loginWithKey(String key, SysUser user) {
        // 查询到成功就保存到用户cookie
        /*
          def parse_credential_url(events: dict) -> Credential:
              url = events["url"]
              cookies_list = url.split("?")[1].split("&")
              sessdata = ""
              bili_jct = ""
              dedeuserid = ""
              for cookie in cookies_list:
                  if cookie[:8] == "SESSDATA":
                      sessdata = cookie[9:]
                  if cookie[:8] == "bili_jct":
                      bili_jct = cookie[9:]
                  if cookie[:11].upper() == "DEDEUSERID=":
                      dedeuserid = cookie[11:]
              ac_time_value = events["refresh_token"]
              buvid3 = get_spi_buvid_sync()["b_3"]
              return Credential(
                  sessdata=sessdata,
                  bili_jct=bili_jct,
                  buvid3=buvid3,
                  dedeuserid=dedeuserid,
                  ac_time_value=ac_time_value,
              )
         */
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(Constant.FULL_BILI_API + "/login/login_with_key?key=" + key),
                okHttpClient);
        /*
        {"code":0,
        "data":{"url":"https://passport.biligame.com/x/passport-login/web/crossDomain?
        DedeUserID=6906052&
        DedeUserID__ckMd5=b315fd91d7ea6429&
        Expires=1727148141&
        SESSDATA=8612c6f2,1727148141,64aee*31CjBBJuas5w5n3IIiyB-SuoXW8U24PTGOzUG_qj5x0ttYehM2GJLvBX0F-242_vW2OCcSVmVVZllFNGk4RF9qYTd4YktYS1ZoUnpmNnI1a3RBUkhfY3V2NUM1cHp1aDRMcnA0SVlzVFR4RV9tYVNRTnhuQ01GUzgwZEE2OTlPaUstaWNLczh2V3RBIIEC&
        bili_jct=9dc72e5c01a78c51569778830e0b7767&
        gourl=https%3A%2F%2Fwww.bilibili.com",
        "refresh_token":"86ca36a2b4e264e1deb21823d93bb531","timestamp":1711596141317,"code":0,"message":""}}
         */

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
            JsonNode spiBuvidSync = getSpiBuvidSync();
            String buvid3 = spiBuvidSync.get("data").get("b_3").asText();
            cookieNode.put("buvid3", buvid3);
            user.setBiliCookies(cookieNode.toString());
            userService.updateById(user);
        }

        return response;
    }
}
