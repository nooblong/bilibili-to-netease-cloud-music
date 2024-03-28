package github.nooblong.download.mq;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.job.JobUtil;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;

import java.util.List;
import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    final PriorityQueue<UploadDetail> queue;
    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final IUserService userService;

    @Value("${powerjob.worker.health-report-interval}")
    private int reportInterval;

    @Value("${main}")
    private Boolean main;

    final OkHttpClient okHttpClient;

    public MusicQueue(BilibiliClient bilibiliClient,
                      NetMusicClient netMusicClient,
                      IUserService userService) {
        this.netMusicClient = netMusicClient;
        this.userService = userService;
        this.okHttpClient = new OkHttpClient();
        this.queue = new PriorityQueue<>();
        this.bilibiliClient = bilibiliClient;
    }

    public void enQueue(UploadDetail uploadDetail) {
        log.info("入队: {}", uploadDetail.getTitle());
        queue.offer(uploadDetail);
    }

    @Override
    public void run() {
        while (true) {
            UploadDetail peek = queue.peek();
            if (peek != null) {
                List<String> list = JobUtil.listWorkersAddrAvailable();
                if (list.isEmpty()) {
                    log.warn("没有可用worker");
                } else {
                    log.info("有可用worker");
                    UploadDetail poll = queue.poll();
                    if (poll != null) {
                        upload(poll, list.get(0));
                        try {
                            Thread.sleep(reportInterval * 1000L + 1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("队列为空");
            }
        }
    }

    private void upload(UploadDetail uploadDetail, String address) {
        Assert.notNull(uploadDetail.getId(), "上传时detailId不应该为空");
        log.info("处理: {}, 优先级: {}, 交给: {}", uploadDetail.getTitle(), uploadDetail.getPriority(), address);
        SaveJobInfoRequest req = new SaveJobInfoRequest();
        ResultDTO<JobInfoDTO> jobInfoDTOResultDTO = JobUtil.powerJobClient.fetchJob(JobUtil.uploadJobId);
        BeanUtils.copyProperties(jobInfoDTOResultDTO, req);
        req.setDispatchStrategyConfig(address);
        JobUtil.powerJobClient.saveJob(req);
        JobUtil.powerJobClient.runJob(JobUtil.uploadJobId,
                JSONUtil.toJsonStr(uploadDetail, JSONConfig.create().setIgnoreNullValue(false)), 0);
//        SaveJobInfoRequest req = new SaveJobInfoRequest();
//        SysUser user = userService.getById(uploadDetail.getUserId());
//        req.setJobName(user.getId() + "-" + uploadDetail.getBvid() + "-" + uploadDetail.getCid());
//        req.setDispatchStrategy(DispatchStrategy.SPECIFY);
//        req.setDesignatedWorkers(address);
//        req.setJobDescription(user.getUsername() + "-" + uploadDetail.getBvid() + "-" + uploadDetail.getVoiceListId());
//        req.setJobParams(JSONUtil.toJsonStr(uploadDetail, JSONConfig.create().setIgnoreNullValue(false)));
//        req.setTimeExpressionType(TimeExpressionType.API);
//        req.setExecuteType(ExecuteType.STANDALONE);
//        req.setProcessorType(ProcessorType.BUILT_IN);
//        req.setProcessorInfo("github.nooblong.download.job.UploadJob");
//        req.setMaxInstanceNum(1);
//        req.setConcurrency(1);
//        req.setInstanceRetryNum(0);
//        req.setTaskRetryNum(0);
//        req.setEnable(false);
//        req.setLogConfig(new LogConfig().setLevel(LogLevel.INFO.getV()).setType(LogType.LOCAL_AND_ONLINE.getV()));
//        req.setInstanceTimeLimit(900000L);
//        req.setDispatchStrategy(DispatchStrategy.HEALTH_FIRST);
//        ResultDTO<Long> saveJob = powerJobClient.saveJob(req);
//        if (!saveJob.isSuccess()) {
//            log.error("请求powerJobClient失败");
//        }
//        powerJobClient.runJob(saveJob.getData());
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (main) {
            new Thread(this).start();
            log.info("开始消费");
        }
    }
}
