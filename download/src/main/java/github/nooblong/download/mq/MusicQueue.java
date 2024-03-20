package github.nooblong.download.mq;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.ResultDTO;

import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable, InitializingBean {

    final PriorityQueue<UploadDetail> queue;
    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    PowerJobClient powerJobClient;
    final IUserService userService;

    @Value("${powerjob.worker.server-address}")
    private String address;

    @Value("${powerjob.worker.app-name}")
    private String name;

    @Value("${powerjob.worker.password}")
    private String password;

    @Value("${main}")
    private Boolean main;

    public MusicQueue(BilibiliClient bilibiliClient,
                      NetMusicClient netMusicClient,
                      IUserService userService) {
        this.netMusicClient = netMusicClient;
        this.userService = userService;
        this.queue = new PriorityQueue<>();
        this.bilibiliClient = bilibiliClient;
    }

    public void enQueue(UploadDetail uploadDetail) {
        queue.offer(uploadDetail);
    }

    @Override
    public void run() {
        while (true) {
            UploadDetail poll = queue.poll();
            if (poll != null) {
                upload(poll);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void upload(UploadDetail uploadDetail) {
        Assert.notNull(uploadDetail.getId(), "上传时detailId不应该为空");
        log.info("处理: {}", uploadDetail);
        SaveJobInfoRequest req = new SaveJobInfoRequest();
        SysUser user = userService.getById(uploadDetail.getUserId());
        req.setJobName(user.getId() + "-" + uploadDetail.getBvid() + "-" + uploadDetail.getCid());
        req.setJobDescription(user.getUsername() + "-" + uploadDetail.getBvid() + "-" + uploadDetail.getVoiceListId());
        req.setJobParams(JSONUtil.toJsonStr(uploadDetail, JSONConfig.create().setIgnoreNullValue(false)));
        req.setTimeExpressionType(TimeExpressionType.API);
        req.setExecuteType(ExecuteType.STANDALONE);
        req.setProcessorType(ProcessorType.BUILT_IN);
        req.setProcessorInfo("github.nooblong.download.job.UploadJob");
        req.setMaxInstanceNum(1);
        req.setConcurrency(1);
        req.setInstanceRetryNum(0);
        req.setTaskRetryNum(0);
        req.setDispatchStrategy(DispatchStrategy.HEALTH_FIRST);
        ResultDTO<Long> saveJob = powerJobClient.saveJob(req);
        if (!saveJob.isSuccess()) {
            log.error("请求powerJobClient失败");
        }
        powerJobClient.runJob(saveJob.getData());
    }

    @Override
    public void afterPropertiesSet() {
        if (main) {
            this.powerJobClient = new PowerJobClient(address, name, password);
            new Thread(this).start();
            log.info("开始消费");
        }
    }
}
