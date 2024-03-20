package github.nooblong.download.mq;

import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable, InitializingBean {

    final PriorityQueue<UploadDetail> queue;
    final BilibiliUtil bilibiliUtil;
    final NetMusicClient netMusicClient;
    final JobLauncher jobLauncher;
    final Job uploadSingleAudioJob;

    public MusicQueue(BilibiliUtil bilibiliUtil, NetMusicClient netMusicClient, JobLauncher jobLauncher, Job uploadSingleAudioJob) {
        this.netMusicClient = netMusicClient;
        this.jobLauncher = jobLauncher;
        this.uploadSingleAudioJob = uploadSingleAudioJob;
        this.queue = new PriorityQueue<>();
        this.bilibiliUtil = bilibiliUtil;
    }

    public void enQueue(UploadDetail uploadDetail) {
        queue.offer(uploadDetail);
    }

    @Override
    public void run() {
        while (true) {
            boolean login3 = bilibiliUtil.isLogin3(bilibiliUtil.getCurrentCred());
            if (!login3) {
                log.info("b站账号未登录");
                try {
                    Thread.sleep(3600000);
                    continue;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            UploadDetail poll = queue.poll();
            if (poll != null) {
                log.info("消费: {}", poll.getTitle());
                boolean login = netMusicClient.checkLogin(poll.getUserId());
                if (!login) {
                    log.info("用户网易云账号过期:用户:{},id:{}", poll.getUserId(), poll.getId());
                    return;
                }
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
        log.info("处理: {}", uploadDetail.getTitle());
    }

    @Override
    public void afterPropertiesSet() {
        new Thread(this).start();
        log.info("开始消费");
    }
}
