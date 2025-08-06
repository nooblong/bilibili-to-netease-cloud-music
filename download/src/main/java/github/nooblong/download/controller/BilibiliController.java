package github.nooblong.download.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.Constant;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.api.QrResponse;
import github.nooblong.download.api.VideoInfoResponse;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.utils.OkUtil;
import okhttp3.OkHttpClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/bilibili")
public class BilibiliController {

    final BilibiliClient bilibiliClient;
    final IUserService userService;
    final RedisTemplate<String, String> redisTemplate;

    public BilibiliController(BilibiliClient bilibiliClient,
                              IUserService userService,
                              RedisTemplate<String, String> redisTemplate) {
        this.bilibiliClient = bilibiliClient;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/getUpChannels")
    public Result<JsonNode> getUpChannels(@RequestParam(name = "upId") String upId) {
        JsonNode upChannels = bilibiliClient.getUpChannels(upId, new HashMap<>());
        return Result.ok("ok", upChannels);
    }

    @GetMapping("/getSelfInfo")
    public Result<JsonNode> getSelfInfo() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        JsonNode upChannels = bilibiliClient.getSelfInfo(userService.getBilibiliCookieMap(sysUser.getId()));
        return Result.ok("ok", upChannels);
    }

    @GetMapping("/getVideoInfo")
    public Result<VideoInfoResponse> getVideoInfo(@RequestParam(name = "bvid") String bvid,
                                                  @RequestParam(required = false, name = "cid") String cid) {
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(bvid);
        simpleVideoInfo.setCid(cid);
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(simpleVideoInfo, availableBilibiliCookie);
//        JsonNode videoStreamUrl = bilibiliClient.getBestStreamUrl(bilibiliFullVideo, availableBilibiliCookie);
//        StringBuilder sb = new StringBuilder();
//        videoStreamUrl.forEach(audio -> {
//            if (audio.has("audio_quality")) {
//                sb.append(AudioQuality.descMap.get(videoStreamUrl.get("audio_quality").asInt()));
//            }
//        });
        VideoInfoResponse videoInfoResponse = new VideoInfoResponse()
                .setImage(bilibiliFullVideo.getVideoInfo().get("data").get("pic").asText())
                .setTitle(bilibiliFullVideo.getTitle())
                .setPages(bilibiliFullVideo.getVideoInfo().get("data").get("pages"))
                .setAuthor(bilibiliFullVideo.getAuthor())
                .setUid(bilibiliFullVideo.getUserId())
//                .setQuality(sb.toString())
                ;
        return Result.ok("查询成功", videoInfoResponse);
    }

    @GetMapping("/getUserInfo")
    public Result<JsonNode> getUserInfo(@RequestParam(name = "uid") String uid) {
        UUID uuid = UUID.randomUUID();
        String formattedUUID = String.format("%s-%s-%s-%s-%s",
                uuid.toString().substring(0, 8),
                uuid.toString().substring(8, 12),
                uuid.toString().substring(12, 16),
                uuid.toString().substring(16, 20),
                uuid.toString().substring(20));
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.
                get(Constant.BAU + "/user/User/get_user_info?uid=" + uid
                        + "&buvid3=" + formattedUUID), new OkHttpClient());
        return Result.ok("查询成功", jsonResponse);
    }

    @GetMapping("/getSeriesInfo")
    public Result<JsonNode> getSeriesInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliClient.getSeriesMeta(id, bilibiliClient.getAndSetBiliCookie());
        return Result.ok("查询成功", seriesMeta1);
    }

    @GetMapping("/getOldSeriesInfo")
    public Result<JsonNode> getOldSeriesInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliClient.getOldSeriesMeta(id, bilibiliClient.getAndSetBiliCookie());
        return Result.ok("查询成功", seriesMeta1);
    }

    @GetMapping("/getFavoriteList")
    public Result<JsonNode> getUserFavoriteList(@RequestParam(name = "uid") String uid) {
        JsonNode favoriteList = bilibiliClient.getUserFavoriteList(uid, bilibiliClient.getAndSetBiliCookie());
        return Result.ok("查询成功", favoriteList);
    }

    @GetMapping("/getSeriesIdByBvid")
    public Result<String> getSeriesIdByBvid(@RequestParam(name = "url") String url) {
        SimpleVideoInfo video = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(url);
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(video, bilibiliClient.getAndSetBiliCookie());
        if (!bilibiliFullVideo.getHasSeries()) {
            return Result.fail("视频没有合集");
        }
        return Result.ok("ok", bilibiliFullVideo.getMySeriesId());
    }

    @PostMapping("/setBiliCookies")
    public Result<JsonNode> setBiliCookies(@RequestBody JsonNode jsonNode) {
        SysUser user = JwtUtil.verifierFromContext();
        user.setBiliCookies(jsonNode.toString());
        Db.updateById(user);
        JsonNode upChannels = bilibiliClient.getSelfInfo(userService.getBilibiliCookieMap(user.getId()));
        return Result.ok("ok", upChannels);
    }

    @GetMapping("/getQrBili")
    public Result<QrResponse> getQrUrl() {
        JwtUtil.verifierFromContext();
        JsonNode data = bilibiliClient.updateQrcodeData();
        String unikey = data.get("data").get("qrcode_key").asText();
        BufferedImage generate = QrCodeUtil.generate(data.get("data").get("url").asText(), 300, 300);
        QrResponse qrResponse = new QrResponse();
        qrResponse.setImage(NetMusicController.bufferedImageToBase64(generate));
        qrResponse.setUniqueKey(unikey);
        return Result.ok("ok", qrResponse);
    }

    @GetMapping("/checkQrBili")
    public Result<JsonNode> getQrUrl(@RequestParam("key") String key) {
        SysUser user = JwtUtil.verifierFromContext();
        return Result.ok("ok", bilibiliClient.loginWithKey(key, user));
    }

    @GetMapping("/allEmoji")
    public Result<JsonNode> allEmoji() {
        ObjectMapper objectMapper = new ObjectMapper();
        String allEmoji = redisTemplate.opsForValue().get("allEmoji");
        if (allEmoji != null) {
            try {
                return Result.ok("ok", objectMapper.readTree(allEmoji));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        Map<String, String> cookie = bilibiliClient.getAndSetBiliCookie();
        JsonNode allEmoji1 = bilibiliClient.getAllEmoji(cookie);
        redisTemplate.opsForValue().set("allEmoji", allEmoji1.toString(), 5, TimeUnit.DAYS);
        return Result.ok("ok", allEmoji1);
    }

    @GetMapping("/emojiDetail")
    public Result<JsonNode> emojiDetail(@RequestParam("id") String id) {
        Map<String, String> cookie = bilibiliClient.getAndSetBiliCookie();
        JsonNode emojiDetail = bilibiliClient.getEmojiDetail(cookie, id);
        return Result.ok("ok", emojiDetail);
    }

}
