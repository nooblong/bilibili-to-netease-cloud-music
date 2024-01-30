package github.nooblong.download.controller;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.api.VideoInfoResponse;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.bilibili.enums.AudioQuality;
import github.nooblong.download.utils.Constant;
import github.nooblong.download.utils.OkUtil;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class BilibiliController {

    final BilibiliUtil bilibiliUtil;
    final OkHttpClient okHttpClient;

    public BilibiliController(BilibiliUtil bilibiliUtil, OkHttpClient okHttpClient) {
        this.bilibiliUtil = bilibiliUtil;
        this.okHttpClient = okHttpClient;
    }

    @GetMapping("/bilibili/checkLogin")
    public Result<Boolean> getLoginStatus() {
        JsonNode likes = bilibiliUtil.getLikes();
        Assert.isTrue(likes.get("code").asInt() == 0, "未登录");
        return Result.ok("已登录", bilibiliUtil.needRefreshCookie());
    }

    @GetMapping("/download/getVideoInfo")
    public Result<VideoInfoResponse> getVideoInfo(@RequestParam(name = "bvid") String bvid,
                                                  @RequestParam(required = false, name = "cid") String cid) {
        BilibiliVideo bilibiliVideo = bilibiliUtil.createByUrl(bvid);
        bilibiliVideo.setCid(cid);
        bilibiliUtil.init(bilibiliVideo);
        JsonNode videoStreamUrl = bilibiliUtil.getBestStreamUrl(bilibiliVideo);
        StringBuilder sb = new StringBuilder();
        videoStreamUrl.forEach(audio -> {
            // todo: 1
            if (audio.has("audio_quality")) {
                sb.append(AudioQuality.descMap.get(videoStreamUrl.get("audio_quality").asInt()));
            }
        });
        VideoInfoResponse videoInfoResponse = new VideoInfoResponse()
                .setImage(bilibiliVideo.getVideoInfo().get("data").get("pic").asText())
                .setTitle(bilibiliVideo.getTitle())
                .setPages(bilibiliVideo.getVideoInfo().get("data").get("pages"))
                .setAuthor(bilibiliVideo.getAuthor())
                .setUid(bilibiliVideo.getUserId())
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
                get("http://" + Constant.FULL_BILI_API + "/user/User/get_user_info?uid=" + uid
                        + "&buvid3=" + formattedUUID), okHttpClient);
        return Result.ok("查询成功", jsonResponse);
    }

    @GetMapping("/download/getSeriesInfo")
    public Result<JsonNode> getFavoriteInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliUtil.getSeriesMeta(id);
        return Result.ok("查询成功", seriesMeta1);
    }

    @GetMapping("/download/getFavoriteList")
    public Result<JsonNode> getUserFavoriteList(@RequestParam(name = "uid") String uid) {
        JsonNode favoriteList = bilibiliUtil.getUserFavoriteList(uid);
        return Result.ok("查询成功", favoriteList);
    }

    @GetMapping("/download/getSeriesIdByBvid")
    public Result<String> getSeriesIdByBvid(@RequestParam(name = "url") String url) {
        BilibiliVideo video = bilibiliUtil.createByUrl(url);
        bilibiliUtil.init(video);
        if (!video.getHasSeries()) {
            return Result.fail("视频没有合集");
        }
        return Result.ok("ok", video.getMySeriesId());
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

}
