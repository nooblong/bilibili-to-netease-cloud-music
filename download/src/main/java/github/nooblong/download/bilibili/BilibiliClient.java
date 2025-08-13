package github.nooblong.download.bilibili;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.bilibili.enums.AudioQuality;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import github.nooblong.common.util.Constant;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.utils.MultiDownload;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Component
public class BilibiliClient {
    final OkHttpClient okHttpClient;
    final IUserService userService;
    final RedisTemplate<String, Map<String, String>> redisTemplate;
    final UploadDetailService uploadDetailService;
    final PythonManager pythonManager;

    public BilibiliClient(IUserService userService,
                          RedisTemplate<String, Map<String, String>> redisTemplate,
                          UploadDetailService uploadDetailService,
                          PythonManager pythonManager) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.uploadDetailService = uploadDetailService;
        this.pythonManager = pythonManager;
        this.okHttpClient = new OkHttpClient.Builder().build();
    }

    public Map<String, String> getAndSetBiliCookie() throws RuntimeException {
        log.info("获取可用b站cookie...");
        String key = "bilibili-cookie";
        Map<String, String> cookieMap = redisTemplate.opsForValue().get(key);
        if (cookieMap != null) {
            return cookieMap;
        }
        List<SysUser> list =
                Db.list(SysUser.class).stream().filter(user -> StrUtil.isNotBlank(user.getBiliCookies())).toList();
        if (list.isEmpty()) {
            throw new RuntimeException("没有用户存在b站cookie");
        }
        for (SysUser sysUser : list) {
            Map<String, String> userCredMap = userService.getBilibiliCookieMap(sysUser.getId());
            boolean login3 = isLogin(userCredMap);
            log.info("检查用户{},cookie状态:{}", sysUser.getUsername(), login3);
            if (login3) {
                log.info("使用用户cookie:{}", sysUser.getUsername());
                redisTemplate.opsForValue().set(key, userCredMap, Duration.ofMinutes(1));
                return userCredMap;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("没有可用b站cookie");
    }

    public BilibiliFullVideo getFullVideoByBvidOrUrl(String bvid, Map<String, String> bilibiliCookie) {
        SimpleVideoInfo byUrl = getSimpleVideoInfoByBvidOrUrl(bvid);
        return getFullVideoBySimpleVideo(byUrl, bilibiliCookie);
    }

    public BilibiliFullVideo getFullVideoBySimpleVideo(SimpleVideoInfo video, Map<String, String> cred) {
        Assert.notNull(video.getBvid(), "bvid为空");
        Assert.isTrue(video.getBvid().toLowerCase().startsWith("bv"), "不是bv开头");
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("get_info");
        builder.addQueryParameter("bvid", video.getBvid());
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        if (response.get("code").asInt() != 0) {
            HttpUrl.Builder builder2 = CommonUtil.getUrlBuilder();
            cred.forEach(builder::addQueryParameter);
            builder2.addPathSegment("video").addPathSegment("Video").addPathSegment("get_info");
            builder2.addQueryParameter("bvid", video.getBvid());
            response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        }
        Assert.notNull(response, "请求视频信息错误");
        Assert.isTrue(response.get("code").asInt() == 0, "请求视频信息错误");
        BilibiliFullVideo bilibiliFullVideo = new BilibiliFullVideo();
        bilibiliFullVideo.setVideoInfo(response);
        bilibiliFullVideo.setSelectCid(video.getCid());
        return bilibiliFullVideo;
    }

    public SimpleVideoInfo getSimpleVideoInfoByBvidOrUrl(String url) {
        if (url.startsWith("http") ||
                url.startsWith("www.") ||
                url.startsWith("b23.tv") ||
                url.startsWith("bilibili")) {
            String bvid;
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
                    bvid = s;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (topPrivateDomain.equals("bilibili.com")) {
                String s = parse.pathSegments().get(1);
                Assert.isTrue(s.toUpperCase().startsWith("BV"), "解析错误");
                bvid = s;
            } else {
                throw new RuntimeException("错误url");
            }
            return new SimpleVideoInfo().setBvid(bvid);
        } else if (url.toLowerCase().startsWith("bv")) {
            return new SimpleVideoInfo().setBvid(url);
        } else {
            throw new RuntimeException("未知的输入");
        }
    }

    public boolean isLogin(Map<String, String> credMap) {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                    + "/user/get_self_info", credMap), okHttpClient);
//            Assert.isTrue(jsonResponse.get("data").get("vip").get("status").asInt() == 1, "不是大会员");
            Assert.isTrue(jsonResponse.get("code").asInt() != -1, "未登录");
            return true;
        } catch (Exception e) {
            log.error("isLogin错误", e);
            return false;
        }
    }

//    ------------------------------Common Api----------------------------

    public JsonNode checkRefresh(Map<String, String> cred) {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                + "/Credential/check_refresh", cred), okHttpClient);
    }

    public JsonNode refresh(Map<String, String> cred) {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU
                + "/Credential/refresh", cred), okHttpClient);
    }

    public List<String> getAudioUrl(BilibiliFullVideo bilibiliFullVideo, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("video").addPathSegment("Video").addPathSegment("get_download_url");
        builder.addQueryParameter("bvid", bilibiliFullVideo.getBvid());
        if (StrUtil.isNotBlank(bilibiliFullVideo.getCid())) {
            builder.addQueryParameter("cid", bilibiliFullVideo.getCid());
        } else {
            builder.addQueryParameter("page_index", "int(0):parse");
        }
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取不到下载链接0");
//        log.info("获取信息: {}", response.toPrettyString());
        ArrayNode audios;
        try {
            audios = ((ArrayNode) response.get("data").get("dash").get("audio"));
            if (overLengthLimit(response.get("data"))) {
                return null;
            }
        } catch (Exception e) {
            log.error("返回了什么? {}", response.toPrettyString());
            log.info("重启python服务!");
            try {
                pythonManager.restart();
                Thread.sleep(5000);
            } catch (IOException | InterruptedException ex) {
                log.error("python服务重启失败");
                throw new RuntimeException(ex);
            }
            JsonNode response2 = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
            if (overLengthLimit(response2.get("data"))) {
                return null;
            }
            audios = ((ArrayNode) response2.get("data").get("dash").get("audio"));
            log.error("返回了什么? {}", response2.toPrettyString());
        }
        log.info(audios.toPrettyString());
        int max = 0;
        boolean hasHiRes = false;
        for (JsonNode audio : audios) {
            if (audio.get("id").asInt() > max) {
                max = audio.get("id").asInt();
            }
            if (audio.get("id").asInt() == AudioQuality.HI_RES.getCode()) {
                hasHiRes = true;
            }
        }
        Assert.isTrue(max != 0, "获取不到下载链接1");
        List<String> urls = new ArrayList<>();
        for (JsonNode audio : audios) {
            if (hasHiRes) {
                if (audio.get("id").asInt() == AudioQuality.HI_RES.getCode()) {
                    urls.add(audio.get("base_url").asText());
                    urls.add(audio.get("backup_url").asText());
                }
            } else {
                if (audio.get("id").asInt() == max) {
                    urls.add(audio.get("base_url").asText());
                    if (audio.has("backup_url")) {
                        ArrayNode backupUrls = (ArrayNode) audio.get("backup_url");
                        for (JsonNode backupUrl : backupUrls) {
                            urls.add(backupUrl.asText());
                        }
                    }
                }
            }
        }
        Assert.isTrue(!urls.isEmpty(), "获取不到下载链接2");
        return urls;
    }

    private boolean overLengthLimit(JsonNode data) {
        int timelength = data.get("timelength").asInt();
        if ((timelength / 1000 / 60 / 60) > 2) {
            log.error("时长超过2小时，请升级ssssssssvip");
            return true;
        }
        return false;
    }

    public JsonNode getUserFavoriteList(String uid, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("favorite_list").addPathSegment("get_video_favorite_list");
        builder.addQueryParameter("uid", uid);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取用户收藏列表失败");
        return response;
    }

    public Path downloadFile(BilibiliFullVideo video, Map<String, String> cred) throws Exception {
        List<String> audioUrl = getAudioUrl(video, cred);
        if (audioUrl.isEmpty()) {
            throw new RuntimeException("audioUrl为空");
        }
        String fileName = video.getBvid();
        Path path = Paths.get(Constant.TMP_FOLDER);
        return doDownloadMulti(path.toString(), fileName, audioUrl, video.getBvid());
    }

    private Path doDownloadMulti(String path, String fileName,
                                 List<String> audioUrl, String bvid) throws Exception {
        File downloadedFile = new File(path, fileName + "-" + UUID.randomUUID().toString().substring(0, 8));
        String referer = "https://bilibili.com/" + bvid;
        try {
            MultiDownload.downloadWithRange(audioUrl.get(0), downloadedFile, 1024 * 512, referer);
        } catch (Exception e) {
            log.error("链接失效: {}", audioUrl.get(0));
            if (audioUrl.size() > 1) {
                log.info("尝试备用链接: {}", audioUrl.get(1));
                try {
                    MultiDownload.downloadWithRange(audioUrl.get(0), downloadedFile, 1024 * 512, referer);
                } catch (Exception e1) {
                    throw new RuntimeException("备用链接失效");
                }
            }
        }
        return downloadedFile.toPath();
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

    public IteratorCollectionTotal getCollectionVideos(String collectionId, int ps, int pn,
                                                       CollectionVideoOrder collectionVideoOrder,
                                                       Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
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

    public IteratorCollectionTotal getOldCollectionVideos(String collectionId, int ps, int pn,
                                                          CollectionVideoOrder collectionVideoOrder, Map<String,
                    String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
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
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
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
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(1):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public JsonNode getOldSeriesMeta(String seriesId, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("channel_series").addPathSegment("ChannelSeries").addPathSegment("get_meta");
        builder.addQueryParameter("id_", seriesId);
        builder.addQueryParameter("type_", "channel_series.ChannelSeriesType(0):parse");
        return OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
    }

    public IteratorCollectionTotal getUpVideos(String upId, int ps, int pn, UserVideoOrder userVideoOrder,
                                               String keyWord, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
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
        if (response.get("code").asInt() != 0) {
            HttpUrl.Builder builder2 = CommonUtil.getUrlBuilder();
            builder2.addPathSegment("user").addPathSegment("User").addPathSegment("get_videos");
            builder2.addQueryParameter("ps", String.valueOf(ps));
            builder2.addQueryParameter("pn", String.valueOf(pn));
            builder2.addQueryParameter("uid", upId);
            if (StrUtil.isNotEmpty(keyWord)) {
                builder2.addQueryParameter("keyword", keyWord);
            }
            builder2.addQueryParameter("order", "user.VideoOrder(\"" + userVideoOrder.getValue() + "\"):parse");
            response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        }
        Assert.notNull(response, "获取Up视频失败");
        Assert.isTrue(response.get("code").asInt() == 0, "获取Up视频失败");
        return new IteratorCollectionTotal()
                .setData((ArrayNode) response.get("data").get("list").get("vlist"))
                .setTotalNum(response.get("data").get("page").get("count").asInt());
    }

    public JsonNode getUpChannels(String upId, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
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
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("get_self_info");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.notNull(response, "获取个人信息失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取个人信息失败");
        return response;
    }

    public JsonNode getUserInfo(String upId, Map<String, String> cred) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        cred.forEach(builder::addQueryParameter);
        builder.addPathSegment("user").addPathSegment("User").addPathSegment("get_user_info");
        builder.addQueryParameter("uid", upId);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        if (response == null || response.get("code").asInt() == -1) {
            HttpUrl parse = HttpUrl.parse(Constant.BAU);
            assert parse != null;
            HttpUrl.Builder builder2 = parse.newBuilder();
            builder2.addPathSegment("user").addPathSegment("User").addPathSegment("get_user_info");
            builder2.addQueryParameter("uid", upId);
            response = OkUtil.getJsonResponse(OkUtil.get(builder2.build()), okHttpClient);
        }
        Assert.notNull(response, "获取Up信息失败");
        Assert.isTrue(response.get("code").asInt() != -1, "获取Up信息失败");
        return response;
    }

    public JsonNode updateQrcodeData() {
        return OkUtil.getJsonResponse(OkUtil.get(Constant.BAU + "/login_v2/QrCodeLogin/generate_qrcode"), okHttpClient);
    }

    public JsonNode loginWithKey(String key, SysUser user) {
        // 查询到成功就保存到用户cookie
        JsonNode response =
                OkUtil.getJsonResponse(OkUtil.get(Constant.BAU + "/login_v2/QrCodeLogin/check_state?key=" + key),
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
                                                                                   UserVideoOrder userVideoOrder,
                                                                                   String keyWord,
                                                                                   Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal collectionTotal = getUpVideos(upId, ps, pn, userVideoOrder, keyWord, bilibiliCookie);
        List<SimpleVideoInfo> data = new ArrayList<>();
        collectionTotal.getData().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(CommonUtil.parseStrTime(jsonNode.get("length").asText()))
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
        SimpleVideoInfo video = getSimpleVideoInfoByBvidOrUrl(bvid);
        BilibiliFullVideo bilibiliFullVideo = getFullVideoBySimpleVideo(video, bilibiliCookie);
        List<SimpleVideoInfo> data = new ArrayList<>();
        bilibiliFullVideo.getPartVideos().forEach(jsonNode -> {
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo()
                    .setDuration(jsonNode.get("duration").asInt())
                    .setBvid(bvid)
                    .setCid(jsonNode.get("cid").asText())
                    .setPartName(jsonNode.get("part").asText())
                    .setTitle(bilibiliFullVideo.getTitle());
            data.add(simpleVideoInfo);
        });
        IteratorCollectionTotalList<SimpleVideoInfo> result = new IteratorCollectionTotalList<>();
        result.setData(data).setTotalNum(data.size());
        return result;
    }

    public IteratorCollectionTotalList<SimpleVideoInfo> getCollectionVideoListFromBilibili(String collectionId,
                                                                                           int ps, int pn,
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

    public IteratorCollectionTotalList<SimpleVideoInfo> getOldCollectionVideoListFromBilibili(String collectionId,
                                                                                              int ps, int pn,
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

    public IteratorCollectionTotalList<SimpleVideoInfo> getFavoriteVideoListFromBilibili(String favoriteId, int page,
                                                                                         Map<String, String> bilibiliCookie) {
        IteratorCollectionTotal favoriteVideos = getFavoriteVideos(favoriteId, page, bilibiliCookie);
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

    public JsonNode getAllEmoji(Map<String, String> bilibiliCookie) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        bilibiliCookie.forEach(builder::addQueryParameter);
        builder.addPathSegment("emoji").addPathSegment("get_all_emoji");
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取emoji信息失败");
        return response.get("data");
    }

    public JsonNode getEmojiDetail(Map<String, String> bilibiliCookie, String emojiId) {
        HttpUrl.Builder builder = CommonUtil.getUrlBuilder();
        bilibiliCookie.forEach(builder::addQueryParameter);
        builder.addPathSegment("emoji").addPathSegment("get_emoji_detail");
        builder.addQueryParameter("id", emojiId);
        JsonNode response = OkUtil.getJsonResponse(OkUtil.get(builder.build()), okHttpClient);
        Assert.isTrue(response.get("code").asInt() != -1, "获取emojiDetail信息失败");
        return response.get("data");
    }
}
