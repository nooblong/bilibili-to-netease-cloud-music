package download;

import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.service.UserVoicelistService;
import github.nooblong.system.SystemStart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

}
