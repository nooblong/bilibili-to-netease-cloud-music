package github.nooblong.download.mq;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.ResultDTO;

import java.util.ArrayList;
import java.util.List;
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

    @Value("${powerjob.worker.health-report-interval}")
    private int reportInterval;

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
        queue.offer(uploadDetail);
    }

    @Override
    public void run() {
        while (true) {
            List<String> list = listWorkersAddrAvailable();
            if (list.isEmpty()) {
                log.warn("没有可用worker");
            } else {
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
        }
    }

    private void upload(UploadDetail uploadDetail, String address) {
        Assert.notNull(uploadDetail.getId(), "上传时detailId不应该为空");
        log.info("处理: {}, 优先级: {}, 交给: {}", uploadDetail.getTitle(), uploadDetail.getPriority(), address);
        SaveJobInfoRequest req = new SaveJobInfoRequest();
        SysUser user = userService.getById(uploadDetail.getUserId());
        req.setJobName(user.getId() + "-" + uploadDetail.getBvid() + "-" + uploadDetail.getCid());
        req.setDispatchStrategy(DispatchStrategy.SPECIFY);
        req.setDesignatedWorkers(address);
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
        req.setEnable(false);
        req.setLogConfig(new LogConfig().setLevel(LogLevel.INFO.getV()).setType(LogType.LOCAL_AND_ONLINE.getV()));
        req.setInstanceTimeLimit(900000L);
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

    public List<String> listWorkersAddrAvailable() {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponseNoLog(
                    OkUtil.getNoLog("http://" + address + "/system/listWorker?appId=1"), okHttpClient);
            Assert.isTrue(jsonResponse.get("success").asBoolean(), "查询worker失败=false");
            ArrayNode data = (ArrayNode) jsonResponse.get("data");
            ArrayList<String> result = new ArrayList<>();
            for (JsonNode datum : data) {
                if (datum.get("status").asInt() != 9999
                && datum.get("lightTaskTrackerNum").asInt() == 0
                && datum.get("heavyTaskTrackerNum").asInt() == 0) {
                    result.add(datum.get("address").asText());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("查询worker失败", e);
        }
        return new ArrayList<>();
    }
}
