package github.nooblong.download;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.HashMap;

@Slf4j
//@SpringBootTest(args = {"--workingDir=/Users/lyl/Documents/GitHub/nosync.nosync/little/workingDir",
//        "--queueTail=",
//        "--initialDelay=10000000000"
//})
@SpringBootTest(args = {"--workingDir=C:\\Users\\lyl\\Documents\\GitHub\\little\\workingDir",
        "--queueTail=",
        "--initialDelay=10000000000"
})
public class BaseTest {

    @Autowired
    public NetMusicClient netMusicClient;
    @Autowired
    public UploadDetailService uploadDetailService;

    @Test
    void time() {
        String time = "03:30";
        LocalTime time1 = LocalTime.parse(time);
        int minute = time1.getMinute();
        int second = time1.getSecond();
        System.out.println(minute);
        System.out.println(second);
    }
}
