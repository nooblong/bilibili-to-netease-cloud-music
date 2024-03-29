package github.nooblong.download;

import github.nooblong.common.service.IUserService;
import github.nooblong.download.mq.MusicQueue;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;

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
    public MusicQueue musicQueue;

    @Test
    void time() throws InterruptedException {
        String time = "03:30";
        LocalTime time1 = LocalTime.parse(time);
        int minute = time1.getMinute();
        int second = time1.getSecond();
        System.out.println(minute);
        System.out.println(second);
        Thread.sleep(2000);
    }
}
