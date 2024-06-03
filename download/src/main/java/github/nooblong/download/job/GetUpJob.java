package github.nooblong.download.job;

import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class GetUpJob {

    final SubscribeService subscribeService;
    final UploadDetailService uploadDetailService;

    public GetUpJob(SubscribeService subscribeService,
                    UploadDetailService uploadDetailService) {
        this.subscribeService = subscribeService;
        this.uploadDetailService = uploadDetailService;
    }

    public void process() {
        log.info("开始获取up主");
        subscribeService.checkAndSave();
    }

}
