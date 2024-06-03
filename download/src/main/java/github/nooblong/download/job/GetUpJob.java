package github.nooblong.download.job;

import github.nooblong.download.mq.MusicQueue;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class GetUpJob {

    final SubscribeService subscribeService;
    final MusicQueue musicQueue;
    final UploadDetailService uploadDetailService;

    public GetUpJob(SubscribeService subscribeService,
                    MusicQueue musicQueue,
                    UploadDetailService uploadDetailService) {
        this.subscribeService = subscribeService;
        this.musicQueue = musicQueue;
        this.uploadDetailService = uploadDetailService;
    }

    public void process() {
        log.info("开始获取up主");
        subscribeService.checkAndSave();
    }

}
