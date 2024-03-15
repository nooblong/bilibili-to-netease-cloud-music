package github.nooblong.download.mq;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable {

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
            try {
                Assert.isTrue(bilibiliUtil.isLogin3(bilibiliUtil.getCurrentCred()), "未登录");
            } catch (Exception e) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                continue;
            }

            UploadDetail poll = queue.poll();
            if (poll != null) {
                log.info("消费: {}", poll.getTitle());

                boolean login = netMusicClient.checkLogin(poll.getUserId());
                if (!login) {
                    log.info("用户网易云账号过期");
                    return;
                }

            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void upload(UploadDetail uploadDetail) {

        // 检查网易、b站登录状态，

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("id", uploadDetail.getId())
                .addString("url", uploadDetail.getBvid())
                .addString("cid", uploadDetail.getCid())
                // music
                .addLong("uploadUserId", uploadDetail.getUserId())
                .addLong("voiceListId", uploadDetail.getVoiceListId())
                .addLong("crack", uploadDetail.getCrack())
                .addLong("useVideoCover", uploadDetail.getUseVideoCover())
                .addDouble("beginSec", uploadDetail.getBeginSec())
                .addDouble("endSec", uploadDetail.getEndSec())
                .addDouble("voiceOffset", uploadDetail.getOffset())
                .addString("customUploadName", uploadDetail.getUploadName())
                .addDate("date", new Date())
                .toJobParameters();
        try {
            jobLauncher.run(uploadSingleAudioJob, jobParameters);
            log.info("处理完毕: {}", uploadDetail.getUploadName());
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("任务已在运行");
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("任务已运行完毕");
        } catch (Exception e) {
            log.error("error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
