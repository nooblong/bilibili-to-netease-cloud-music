package github.nooblong.download.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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
//        PriorityQueue<UploadDetail> copy = new PriorityQueue<>(queue);
        List<UploadDetail> result = new ArrayList<>();
        UploadDetail[] array = queue.toArray(new UploadDetail[0]);
        for (UploadDetail uploadDetail : array) {
            uploadDetail.setMergeTitle(
                    StrUtil.isNotBlank(uploadDetail.getUploadName()) ? uploadDetail.getUploadName() : uploadDetail.getTitle());
        }
        Collections.addAll(result, array);
//        while (true) {
//            UploadDetail poll = copy.poll();
//            if (poll != null) {
//                poll.setMergeTitle(StrUtil.isNotBlank(poll.getUploadName()) ? poll.getUploadName() : poll.getTitle());
//                result.add(poll);
//            } else {
//                break;
//            }
//        }
        return result;
    }

    @Override
    public void run() {
        try {
            while (true) {

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
