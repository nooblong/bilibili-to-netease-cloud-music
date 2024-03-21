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
        {"id":null,"jobName":"GetUpJob_EXPORT_1710991920759","jobDescription":"获取up主","appId":1,"jobParams":null,"timeExpressionType":"CRON","timeExpression":"0 0 0/1 * * ? ","executeType":"BROADCAST","processorType":"BUILT_IN","processorInfo":"github.nooblong.download.job.GetUpJob","maxInstanceNum":0,"concurrency":1,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":1,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true,"designatedWorkers":"","maxWorkerCount":0,"notifyUserIds":null,"extra":null,"dispatchStrategy":"HEALTH_FIRST","dispatchStrategyConfig":null,"lifeCycle":{"start":null,"end":null},"alarmConfig":{"alertThreshold":0,"statisticWindowLen":0,"silenceWindowLen":0},"tag":null,"logConfig":{"type":4,"level":null,"loggerName":null},"advancedRuntimeConfig":{"taskTrackerBehavior":null}}
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
