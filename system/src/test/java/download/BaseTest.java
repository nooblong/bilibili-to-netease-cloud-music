package download;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.service.UserVoicelistService;
import github.nooblong.system.SystemStart;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest(properties = {"spring.config.location=classpath:application-local.yml"}, classes = SystemStart.class)
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
    void testUpload() {
        UploadDetail uploadDetail = new UploadDetail();
        uploadDetail.setId(999999999L);
        uploadDetail.setUserId(53L);
        uploadDetail.setVoiceListId(998889515L);
        uploadDetail.setSubscribeId(177L);
        uploadDetail.setBvid("BV1P8tGzbE4a");
        uploadDetail.setCid("31482908075");
        uploadDetail.setBeginSec(10D);
        uploadDetail.setEndSec(70D);
        uploadDetail.setOffset(-20D);
//        uploadDetail.setUploadName("test");
        uploadDetail.setUseVideoCover(1L);
        Db.removeById(uploadDetail.getId(), UploadDetail.class);
        Db.save(uploadDetail);
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        uploadJob.process(999999999L, availableBilibiliCookie);
    }

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
