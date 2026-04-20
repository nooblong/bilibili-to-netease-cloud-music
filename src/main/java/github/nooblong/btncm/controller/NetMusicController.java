package github.nooblong.btncm.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.entity.Result;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.utils.CommonUtil;
import github.nooblong.btncm.utils.JwtUtil;
import github.nooblong.btncm.entity.QrResponse;
import github.nooblong.btncm.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 网易云音乐接口
 */
@RestController
@RequestMapping("/netmusic")
@Slf4j
public class NetMusicController {

    final NetMusicClient netMusicClient;
    final IUserService userService;

    public NetMusicController(NetMusicClient netMusicClient, IUserService userService) {
        this.netMusicClient = netMusicClient;
        this.userService = userService;
    }

    /**
     * 登录状态
     */
    @GetMapping("/loginStatus")
    public Result<JsonNode> getloginStatus() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        JsonNode loginStatus = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginStatus", sysUser.getId());
        return Result.ok("ok", loginStatus);
    }

    /**
     * 设置网易云cookie
     */
    @PostMapping("/setNetCookie")
    public Result<JsonNode> setNetCookie(@RequestBody JsonNode json) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        String cookie = json.toString();
        Map<String, String> map = CommonUtil.convertJsonToMap(cookie);
        userService.updateNeteaseCookieByCookieMap(sysUser.getId(), map);
        JsonNode loginStatus = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginStatus", sysUser.getId());
        return Result.ok("ok", loginStatus);
    }

    /**
     * 获取登录二维码
     */
    @GetMapping("/getQrCode")
    public Result<QrResponse> getQrCode() {
        JsonNode loginQrKey;
        SysUser sysUser = JwtUtil.verifierFromContext();
        loginQrKey = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginQrKey", sysUser.getId());
        String unikey = loginQrKey.get("unikey").asText();
        BufferedImage generate = QrCodeUtil.generate("https://music.163.com/login?codekey=" +
                unikey + "&chainId=" + generateChainId(null), 300, 300);
        QrResponse qrResponse = new QrResponse();
        qrResponse.setImage(bufferedImageToBase64(generate));
        qrResponse.setUniqueKey(unikey);
        return Result.ok("ok", qrResponse);
    }

    /**
     * 二维码登录相关
     */
    private static String generateChainId(String cookie) {
        String version = "v1";

        int randomNum = ThreadLocalRandom.current().nextInt(1_000_000);

        String deviceId = getCookieValue(cookie, "sDeviceId");
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "unknown-" + randomNum;
        }

        String platform = "web";
        String action = "login";
        long timestamp = System.currentTimeMillis();

        return String.format("%s_%s_%s_%s_%d",
                version, deviceId, platform, action, timestamp);
    }

    /**
     * 二维码登录相关
     */
    private static String getCookieValue(String cookieStr, String name) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return null;
        }

        String cookies = "; " + cookieStr;
        String target = "; " + name + "=";

        String[] parts = cookies.split(target);

        if (parts.length == 2) {
            String lastPart = parts[1];
            int endIndex = lastPart.indexOf(";");
            if (endIndex != -1) {
                return lastPart.substring(0, endIndex);
            }
            return lastPart;
        }

        return null;
    }

    /**
     * BufferedImage 编码转换为 base64
     */
    public static String bufferedImageToBase64(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();//io流
        try {
            ImageIO.write(bufferedImage, "png", baos);//写入流中
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        byte[] bytes = baos.toByteArray();//转换成字节
        String pngBase64 = Base64.encode(bytes);//转换成base64串
        pngBase64 = pngBase64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
        return "data:image/jpg;base64," + pngBase64;
    }

}
