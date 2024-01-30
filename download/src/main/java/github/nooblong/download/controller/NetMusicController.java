package github.nooblong.download.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.model.Result;
import github.nooblong.download.api.QrResponse;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

@RestController
@Slf4j
public class NetMusicController {

    final NetMusicClient netMusicClient;
    final OkHttpClient okHttpClient;

    public NetMusicController(NetMusicClient netMusicClient, OkHttpClient okHttpClient) {
        this.netMusicClient = netMusicClient;
        this.okHttpClient = okHttpClient;
    }

    @GetMapping("/netmusic/loginStatus")
    public Result<?> getloginStatus() {
        JsonNode loginstatus;
        try {
            loginstatus = netMusicClient.getMusicDataByContext(new HashMap<>(), "loginstatus");
        } catch (ValidateException | IllegalArgumentException e) {
            log.error(e.getMessage());
            return Result.fail("连接网易失败");
        }
        return Result.ok("ok", loginstatus);
    }

    @GetMapping("/netmusic/getQrCode")
    public Result<QrResponse> getQrCode() {
        JsonNode loginqrkey;
        try {
            loginqrkey = netMusicClient.getMusicDataByContext(new HashMap<>(), "loginqrkey");
        } catch (ValidateException | IllegalArgumentException e) {
            log.error(e.getMessage());
            return Result.fail("连接网易失败");
        }
        String unikey = loginqrkey.get("unikey").asText();
        BufferedImage generate = QrCodeUtil.generate("https://music.163.com/login?codekey=" +
                unikey, 300, 300);
        QrResponse qrResponse = new QrResponse();
        qrResponse.setImage(bufferedImageToBase64(generate));
        qrResponse.setUniqueKey(unikey);
        return Result.ok("ok", qrResponse);
    }

    /**
     * BufferedImage 编码转换为 base64
     */
    private static String bufferedImageToBase64(BufferedImage bufferedImage) {
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
