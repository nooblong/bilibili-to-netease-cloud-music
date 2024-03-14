package github.nooblong.download;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import java.time.LocalTime;
import java.util.HashMap;

@Slf4j
@SpringBootTest(properties = {"spring.config.location=classpath:application-local.yml"})
public class BaseTest {

    @Autowired
    public NetMusicClient netMusicClient;
    @Autowired
    public UploadDetailService uploadDetailService;

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
