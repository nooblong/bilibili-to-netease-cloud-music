package github.nooblong.download.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.api.QrResponse;
import github.nooblong.download.api.VideoInfoResponse;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.bilibili.enums.AudioQuality;
import github.nooblong.download.utils.Constant;
import github.nooblong.download.utils.OkUtil;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.util.UUID;

@RestController
public class BilibiliController {

    final BilibiliClient bilibiliClient;

    public BilibiliController(BilibiliClient bilibiliClient) {
        this.bilibiliClient = bilibiliClient;
    }

    @GetMapping("/bilibili/checkLogin")
    public Result<Boolean> checkLogin() {
        return Result.ok("执行成功", !bilibiliClient.getCurrentCred().isEmpty());
    }

    @GetMapping("/download/getVideoInfo")
    public Result<VideoInfoResponse> getVideoInfo(@RequestParam(name = "bvid") String bvid,
                                                  @RequestParam(required = false, name = "cid") String cid) {
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(bvid);
        simpleVideoInfo.setCid(cid);
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
        JsonNode videoStreamUrl = bilibiliClient.getBestStreamUrl(bilibiliFullVideo, bilibiliClient.getCurrentCred());
        StringBuilder sb = new StringBuilder();
        videoStreamUrl.forEach(audio -> {
            // todo: 1
            if (audio.has("audio_quality")) {
                sb.append(AudioQuality.descMap.get(videoStreamUrl.get("audio_quality").asInt()));
            }
        });
        VideoInfoResponse videoInfoResponse = new VideoInfoResponse()
                .setImage(bilibiliFullVideo.getVideoInfo().get("data").get("pic").asText())
                .setTitle(bilibiliFullVideo.getTitle())
                .setPages(bilibiliFullVideo.getVideoInfo().get("data").get("pages"))
                .setAuthor(bilibiliFullVideo.getAuthor())
                .setUid(bilibiliFullVideo.getUserId())
                .setQuality(sb.toString());
        return Result.ok("查询成功", videoInfoResponse);
    }

    @GetMapping("/download/getUserInfo")
    public Result<JsonNode> getUserInfo(@RequestParam(name = "uid") String uid) {
        UUID uuid = UUID.randomUUID();
        String formattedUUID = String.format("%s-%s-%s-%s-%s",
                uuid.toString().substring(0, 8),
                uuid.toString().substring(8, 12),
                uuid.toString().substring(12, 16),
                uuid.toString().substring(16, 20),
                uuid.toString().substring(20));
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.
                get(Constant.FULL_BILI_API + "/user/User/get_user_info?uid=" + uid
                        + "&buvid3=" + formattedUUID), new OkHttpClient());
        return Result.ok("查询成功", jsonResponse);
    }

    @GetMapping("/download/getSeriesInfo")
    public Result<JsonNode> getFavoriteInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliClient.getSeriesMeta(id, bilibiliClient.getCurrentCred());
        return Result.ok("查询成功", seriesMeta1);
    }

    @GetMapping("/download/getFavoriteList")
    public Result<JsonNode> getUserFavoriteList(@RequestParam(name = "uid") String uid) {
        JsonNode favoriteList = bilibiliClient.getUserFavoriteList(uid, bilibiliClient.getCurrentCred());
        return Result.ok("查询成功", favoriteList);
    }

    @GetMapping("/download/getSeriesIdByBvid")
    public Result<String> getSeriesIdByBvid(@RequestParam(name = "url") String url) {
        SimpleVideoInfo video = bilibiliClient.createByUrl(url);
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(video, bilibiliClient.getCurrentCred());
        if (!bilibiliFullVideo.getHasSeries()) {
            return Result.fail("视频没有合集");
        }
        return Result.ok("ok", bilibiliFullVideo.getMySeriesId());
    }

    @PostMapping("/setBiliCookies")
    public Result<String> setBiliCookies(@RequestBody JsonNode jsonNode, HttpServletRequest request) {
        String token = request.getHeader("Access-Token");
        SysUser user = JwtUtil.verifierToken(token);
        Assert.isTrue(user.getId().intValue() == 1, "你为什么要这么做?");
        Assert.isTrue(jsonNode.has("sessdata"), "?");
        Assert.isTrue(jsonNode.has("bili_jct"), "?");
        Assert.isTrue(jsonNode.has("buvid3") || jsonNode.has("buvid4"), "?");
        Assert.isTrue(jsonNode.has("buvid3") || jsonNode.has("buvid4"), "?");
        user.setBiliCookies(jsonNode.toString());
        Db.updateById(user);
        return Result.ok("设置成功");
    }

    @GetMapping("/getBiliCookies")
    public Result<String> getBiliCookies(HttpServletRequest request) {
        String token = request.getHeader("Access-Token");
        SysUser user = JwtUtil.verifierToken(token);
        Assert.isTrue(user.getId().intValue() == 1, "你为什么要这么做?");
        return Result.ok(user.getBiliCookies());
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

}
