package github.nooblong.download.job;

import github.nooblong.download.bilibili.BilibiliClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.log.OmsLogger;


@Slf4j
@Component
public class CheckBilibiliCookieJob implements BroadcastProcessor {

    /*
    {"id":null,"jobName":"CheckBilibiliCookie_EXPORT_1711118000409","jobDescription":null,"appId":1,"jobParams":null,"timeExpressionType":"CRON","timeExpression":"0 0/1 * * * *","executeType":"BROADCAST","processorType":"BUILT_IN","processorInfo":"github.nooblong.download.job.CheckBilibiliCookieJob","maxInstanceNum":0,"concurrency":1,"instanceTimeLimit":0,"instanceRetryNum":0,"taskRetryNum":0,"minCpuCores":0,"minMemorySpace":0,"minDiskSpace":0,"enable":true,"designatedWorkers":"","maxWorkerCount":0,"notifyUserIds":null,"extra":null,"dispatchStrategy":"HEALTH_FIRST","dispatchStrategyConfig":null,"lifeCycle":{"start":null,"end":null},"alarmConfig":{"alertThreshold":0,"statisticWindowLen":0,"silenceWindowLen":0},"tag":null,"logConfig":{"type":4,"level":null,"loggerName":null},"advancedRuntimeConfig":{"taskTrackerBehavior":null}}
     */

    final BilibiliClient bilibiliClient;

    public CheckBilibiliCookieJob(BilibiliClient bilibiliClient) {
        this.bilibiliClient = bilibiliClient;
    }

    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        bilibiliClient.checkCurrentCredMap(omsLogger);
        omsLogger.info("检查b站Cookie任务成功");
        return new ProcessResult(true, "检查b站Cookie任务成功");
    }
}
