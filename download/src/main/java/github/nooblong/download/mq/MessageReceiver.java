package github.nooblong.download.mq;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.rabbitmq.client.Channel;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
public class MessageReceiver {

    final JobLauncher jobLauncher;
    final Job uploadSingleAudioJob;
    final NetMusicClient netMusicClient;
    final BilibiliUtil bilibiliUtil;

    public MessageReceiver(JobLauncher jobLauncher, @Qualifier("uploadSingleAudioJob") Job uploadSingleAudioJob,
                           NetMusicClient netMusicClient,
                           BilibiliUtil bilibiliUtil) {
        this.jobLauncher = jobLauncher;
        this.uploadSingleAudioJob = uploadSingleAudioJob;
        this.netMusicClient = netMusicClient;
        this.bilibiliUtil = bilibiliUtil;
    }

    @RabbitListener(queues = "#{uploadQueue.name}", containerFactory = "jsonContainerFactory")
    public void receiveMessage(Message message, Long id, Channel channel) throws IOException {
//        try {
//            log.info("消费: {}, 优先级: {}", id, message.getMessageProperties().getPriority());
//            Thread.sleep(1000);
//            if (1 + 1 == 2) {
//                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//                return;
//            }
//        } catch (InterruptedException e) {
//            return;
//        }
        UploadDetail byId = Db.getById(id, UploadDetail.class);
        if (byId == null) {
            log.warn("没有detail id: {}", id);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
        log.info("队列处理: {}, {}, {}", byId.getTitle(), byId.getBvid(), byId.getCid());
        boolean login = netMusicClient.checkLogin(byId.getUserId());
        if (!login) {
            byId.setStatus("NETEASE_ACCOUNT_EXPIRED");
            log.info("用户网易云账号过期");
            Db.updateById(byId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            Assert.isTrue(bilibiliUtil.getLoginRole3(), "未登录");
        } catch (Exception e) {
            SysUser sysUser = Db.getById(1, SysUser.class);
            bilibiliUtil.getCredMapByUser(sysUser);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            log.info("b站账号过期");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return;
        }

        try {
            Assert.notNull(byId, "处理时未找到数据: " + id);
            Assert.isTrue(byId.getStatus().equals("NOT_UPLOAD"), "该歌曲已处理");
            upload(byId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // todo: 处理失败先跳过
            log.error("队列处理失败: {}", e.getMessage());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = "#{deadUploadQueue.name}", containerFactory = "simpleContainerFactory")
    public void processFailedMessages(Message message, String content, Channel channel) {
        log.info("收到死信: {}", content);
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
