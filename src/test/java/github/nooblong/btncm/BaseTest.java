package github.nooblong.btncm;

import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.job.UploadJob;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.service.UserVoicelistService;
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
