package github.nooblong.download.mq;

import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable, InitializingBean {

    final PriorityQueue<UploadDetail> queue;
    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final JobLauncher jobLauncher;
    final Job uploadSingleAudioJob;

    public MusicQueue(BilibiliClient bilibiliClient, NetMusicClient netMusicClient, JobLauncher jobLauncher, Job uploadSingleAudioJob) {
        this.netMusicClient = netMusicClient;
        this.jobLauncher = jobLauncher;
        this.uploadSingleAudioJob = uploadSingleAudioJob;
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
        log.info("处理: {}", uploadDetail.getTitle());
    }

    @Override
    public void afterPropertiesSet() {
        new Thread(this).start();
        log.info("开始消费");
    }
}
