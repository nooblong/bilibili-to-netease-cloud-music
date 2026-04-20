package github.nooblong.btncm.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.entity.ExpiringCache;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.entity.Result;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.utils.Constant;
import github.nooblong.btncm.utils.JwtUtil;
import github.nooblong.btncm.entity.QrResponse;
import github.nooblong.btncm.bilibili.VideoInfoResponse;
import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.bilibili.BilibiliFullVideo;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.utils.OkUtil;
import okhttp3.OkHttpClient;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * b站接口
 */
@RestController
@RequestMapping("/bilibili")
public class BilibiliController {

    final BilibiliClient bilibiliClient;
    final IUserService userService;

    public BilibiliController(BilibiliClient bilibiliClient,
                              IUserService userService) {
        this.bilibiliClient = bilibiliClient;
        this.userService = userService;
    }

    /**
     * 获取up主的合集列表
     */
    @GetMapping("/getUpChannels")
    public Result<JsonNode> getUpChannels(@RequestParam(name = "upId") String upId) {
        JsonNode upChannels = bilibiliClient.getUpChannels(upId, new HashMap<>());
        return Result.ok("ok", upChannels);
    }

    /**
     * 获取当前账号的b站信息
     */
    @GetMapping("/getSelfInfo")
    public Result<JsonNode> getSelfInfo() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        JsonNode upChannels = bilibiliClient.getSelfInfo(userService.getBilibiliCookieMap(sysUser.getId()));
        return Result.ok("ok", upChannels);
    }

    /**
     * 根据bvid获取视频信息
     */
    @GetMapping("/getVideoInfo")
    public Result<VideoInfoResponse> getVideoInfo(@RequestParam(name = "bvid") String bvid,
                                                  @RequestParam(required = false, name = "cid") String cid) {
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(bvid);
        simpleVideoInfo.setCid(cid);
        Map<String, String> availableBilibiliCookie = bilibiliClient.getBilibiliCookie();
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(simpleVideoInfo, availableBilibiliCookie);
        VideoInfoResponse videoInfoResponse = new VideoInfoResponse()
                .setImage(bilibiliFullVideo.getVideoInfo().get("data").get("pic").asText())
                .setTitle(bilibiliFullVideo.getTitle())
                .setPages(bilibiliFullVideo.getVideoInfo().get("data").get("pages"))
                .setAuthor(bilibiliFullVideo.getAuthor())
                .setUid(bilibiliFullVideo.getUserId());
        return Result.ok("查询成功", videoInfoResponse);
    }

    /**
     * 根据uid获取用户信息
     */
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

    /**
     * 根据合集id获取视频列表
     */
    @GetMapping("/getSeriesInfo")
    public Result<JsonNode> getSeriesInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliClient.getSeriesMeta(id, bilibiliClient.getBilibiliCookie());
        return Result.ok("查询成功", seriesMeta1);
    }

    /**
     * 根据旧合集id获取视频列表
     */
    @GetMapping("/getOldSeriesInfo")
    public Result<JsonNode> getOldSeriesInfo(@RequestParam(name = "id") String id) {
        JsonNode seriesMeta1 = bilibiliClient.getOldSeriesMeta(id, bilibiliClient.getBilibiliCookie());
        return Result.ok("查询成功", seriesMeta1);
    }

    /**
     * 根据收藏夹id获取视频列表
     */
    @GetMapping("/getFavoriteList")
    public Result<JsonNode> getUserFavoriteList(@RequestParam(name = "uid") String uid) {
        JsonNode favoriteList = bilibiliClient.getUserFavoriteList(uid, bilibiliClient.getBilibiliCookie());
        return Result.ok("查询成功", favoriteList);
    }

    /**
     * 根据视频id获取视频属于的合集
     */
    @GetMapping("/getSeriesIdByBvid")
    public Result<String> getSeriesIdByBvid(@RequestParam(name = "url") String url) {
        SimpleVideoInfo video = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(url);
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(video, bilibiliClient.getBilibiliCookie());
        if (!bilibiliFullVideo.getHasSeries()) {
            return Result.fail("视频没有合集");
        }
        return Result.ok("ok", bilibiliFullVideo.getMySeriesId());
    }

    /**
     * 设置当前用户的b站cookie
     */
    @PostMapping("/setBiliCookies")
    public Result<JsonNode> setBiliCookies(@RequestBody JsonNode jsonNode) {
        SysUser user = JwtUtil.verifierFromContext();
        user.setBiliCookies(jsonNode.toString());
        Db.updateById(user);
        JsonNode upChannels = bilibiliClient.getSelfInfo(userService.getBilibiliCookieMap(user.getId()));
        return Result.ok("ok", upChannels);
    }

    /**
     * 获取登录二维码
     */
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

    /**
     * 二维码登录
     */
    @GetMapping("/checkQrBili")
    public Result<JsonNode> getQrUrl(@RequestParam("key") String key) {
        SysUser user = JwtUtil.verifierFromContext();
        return Result.ok("ok", bilibiliClient.loginWithKey(key, user));
    }

    public ExpiringCache<JsonNode> emojiList = new ExpiringCache<>(72 *60 * 60 * 1000L, this::getAllEmoji);

    /**
     * 获取emoji列表
     */
    @GetMapping("/allEmoji")
    public Result<JsonNode> allEmoji() {
        return Result.ok("ok", emojiList.get());
    }

    public JsonNode getAllEmoji() {
        Map<String, String> cookie = bilibiliClient.getBilibiliCookie();
        return bilibiliClient.getAllEmoji(cookie);
    }

    /**
     * 获取emoji图片
     */
    @GetMapping("/emojiDetail")
    public Result<JsonNode> emojiDetail(@RequestParam("id") String id) {
        Map<String, String> cookie = bilibiliClient.getBilibiliCookie();
        JsonNode emojiDetail = bilibiliClient.getEmojiDetail(cookie, id);
        return Result.ok("ok", emojiDetail);
    }

}
