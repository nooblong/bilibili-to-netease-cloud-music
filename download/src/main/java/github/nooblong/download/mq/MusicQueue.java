package github.nooblong.download.mq;

import cn.hutool.core.util.StrUtil;
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
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable, ApplicationListener<ContextRefreshedEvent> {

    private final PriorityQueue<UploadDetail> queue;
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
        synchronized (this) {
            log.info("入队: {}", uploadDetail.getTitle());
            queue.offer(uploadDetail);
        }
    }

    public List<UploadDetail> listAllQueue() {
        PriorityQueue<UploadDetail> copy = new PriorityQueue<>(queue);
        List<UploadDetail> result = new ArrayList<>();
        while (true) {
            UploadDetail poll = copy.poll();
            if (poll != null) {
                poll.setMergeTitle(StrUtil.isNotBlank(poll.getUploadName()) ? poll.getUploadName() : poll.getTitle());
                result.add(poll);
            } else {
                break;
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public void run() {
        try {
            while (true) {
                UploadDetail peek = queue.peek();
                if (peek != null) {
                    List<String> list = JobUtil.listWorkersAddrAvailable();
                    if (list.isEmpty()) {
//                    log.warn("没有可用worker");
                    } else {
                        synchronized (this) {
                            if (!bilibiliClient.isLogin3(bilibiliClient.getCurrentCred())) {
                                log.info("no cookie sleep");
                                Thread.sleep(60000);
                                continue;
                            }
                            log.info("有可用worker");
                            UploadDetail poll = queue.poll();
                            if (poll != null) {
                                upload(poll, list.get(0));
//                                log.info("队列长度: {}", queue.size());
                                log.info("------>从队列取出: {}:{}", poll.getTitle(), poll.getUploadName());
                                Thread.sleep(reportInterval * 1000L + 1);
                                continue;
                            }
                        }
                    }
                    Thread.sleep(1000);
                } else {
                    Thread.sleep(1000);
//                log.info("队列为空");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (main) {
            new Thread(this).start();
            log.info("开始消费");
        }
    }
}
