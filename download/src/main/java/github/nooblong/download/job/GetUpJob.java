package github.nooblong.download.job;

import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mq.MusicQueue;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.List;


@Slf4j
@Component
public class GetUpJob implements BroadcastProcessor {

    /*

     */

    @Value("${main}")
    private Boolean main;

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

    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        if (!main) {
            omsLogger.info("非主节点跳过该任务");
            return new ProcessResult(true, "非主节点跳过该任务");
        }
        String param;
        // 解析参数，非处于工作流中时，优先取实例参数（允许动态[instanceParams]覆盖静态参数[jobParams]）
        if (context.getWorkflowContext() == null) {
            param = StringUtils.isBlank(context.getInstanceParams()) ? context.getJobParams() : context.getInstanceParams();
        } else {
            param = context.getJobParams();
        }
        omsLogger.info("开始获取up主,param:{}", param);
        try {
            subscribeService.checkAndSave(omsLogger);
            List<UploadDetail> toProcess = uploadDetailService.listAllWait();
            toProcess.forEach(musicQueue::enQueue);
        } catch (Exception e) {
            return new ProcessResult(false, e.getMessage());
        }
        omsLogger.info("获取up主任务成功");
        return new ProcessResult(true, "获取up主任务成功");
    }
}
