package github.nooblong.btncm.job;

import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 获取所有订阅的up主视频任务
 */
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
        log.info("开始检查up主");
        subscribeService.checkAndSave();
    }

}
