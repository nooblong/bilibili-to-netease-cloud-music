package github.nooblong.download.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Collections;

@Slf4j
@Component
public class GetUpJob implements BroadcastProcessor {

    /*
    {"id":null,"jobName":"GetUpJob_EXPORT_1710900842909","jobDescription":"获取up主","appId":1,"jobParams":null,
    "timeExpressionType":"CRON","timeExpression":"0 0 0/1 * * ? ","executeType":"STANDALONE","processorType":"BUILT_IN",
    "processorInfo":"github.nooblong.download.job.GetUpJob","maxInstanceNum":1,"concurrency":1,"instanceTimeLimit":0,
    "instanceRetryNum":0,"taskRetryNum":1,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true,
    "designatedWorkers":"","maxWorkerCount":0,"notifyUserIds":null,"extra":null,"dispatchStrategy":"HEALTH_FIRST",
    "dispatchStrategyConfig":null,"lifeCycle":{"start":null,"end":null},"alarmConfig":{"alertThreshold":0,
    "statisticWindowLen":0,"silenceWindowLen":0},"tag":null,"logConfig":{"type":4,"level":null,"loggerName":null},
    "advancedRuntimeConfig":{"taskTrackerBehavior":null}}
     */

    @Value("${main}")
    private Boolean main;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        if (!main) {
            omsLogger.info("非主节点跳过该任务");
            return new ProcessResult(true, "非主节点跳过该任务");
        }
        omsLogger.info("StandaloneProcessorDemo start process,context is {}.", context);
        omsLogger.info("Notice! If you want this job process failed, your jobParams need to be 'failed'");
        omsLogger.info("Let's test the exception~");
        // 测试异常日志
        try {
            Collections.emptyList().add("277");
        } catch (Exception e) {
            omsLogger.error("oh~it seems that we have an exception~", e);
        }
        log.info("================ StandaloneProcessorDemo#process ================");
        log.info("jobParam:{}", context.getJobParams());
        log.info("instanceParams:{}", context.getInstanceParams());
        String param;
        // 解析参数，非处于工作流中时，优先取实例参数（允许动态[instanceParams]覆盖静态参数[jobParams]）
        if (context.getWorkflowContext() == null) {
            param = StringUtils.isBlank(context.getInstanceParams()) ? context.getJobParams() : context.getInstanceParams();
        } else {
            param = context.getJobParams();
        }
        // 根据参数判断是否成功
        boolean success = !"failed".equals(param);
        omsLogger.info("StandaloneProcessorDemo finished process,success: {}", success);
        omsLogger.info("anyway, we finished the job successfully~Congratulations!");
        return new ProcessResult(success, context + ": " + success);
    }
}
