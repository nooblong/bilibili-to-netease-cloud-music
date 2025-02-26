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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    public RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedis() {
        String redisKey = "user:info";

        System.out.println(redisTemplate.opsForValue().get("asd"));

        // 1. 创建 Map
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", "张三");
        userMap.put("age", "30");
        userMap.put("city", "北京");

        // 2. 存储 Map
//        redisTemplate.opsForValue().set(redisKey, userMap, 30, TimeUnit.SECONDS);
        redisTemplate.opsForHash().putAll(redisKey, userMap);
        redisTemplate.expire(redisKey, 30, TimeUnit.SECONDS);
        System.out.println("Map 数据已存储！");

        // 3. 读取 Map
//        Map<String, String> map = redisTemplate.opsForValue().get(redisKey);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> entries = ops.entries(redisKey);
        System.out.println("读取的 Map 数据：" + entries);

        // 4. 读取特定字段
        Object o = redisTemplate.opsForHash().get(redisKey, "name");
        System.out.println("读取的 name：" + o);

        // 5. 删除 Map
        // redisService.deleteMap(redisKey);
        // System.out.println("Map 数据已删除！");
    }

}
