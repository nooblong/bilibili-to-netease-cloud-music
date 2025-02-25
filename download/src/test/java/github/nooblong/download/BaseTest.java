package github.nooblong.download;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.service.UserVoicelistService;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest(properties = {"spring.config.location=classpath:application-local.yml"})
public class BaseTest {

    @Autowired
    public NetMusicClient netMusicClient;
    @Autowired
    public UploadDetailService uploadDetailService;
    @Autowired
    public IUserService userService;
    @Autowired
    public BilibiliClient bilibiliClient;
    @Autowired
    public UserVoicelistService userVoicelistService;
    @Autowired
    public SubscribeService subscribeService;
    @Autowired
    public UploadJob uploadJob;

}
