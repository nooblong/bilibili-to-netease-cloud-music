package github.nooblong.download.mq;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.entity.UploadDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.PriorityQueue;

@Slf4j
@Component
public class MusicQueue implements Runnable {

    private final PriorityQueue<UploadDetail> queue;
    private final BilibiliUtil bilibiliUtil;

    public MusicQueue(BilibiliUtil bilibiliUtil) {
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
                continue;
            }

            UploadDetail poll = queue.poll();
            if (poll != null) {
                log.info("消费: {}", poll.getTitle());

                boolean login = netMusicClient.checkLogin(byId.getUserId());
                if (!login) {
                    byId.setStatus("NETEASE_ACCOUNT_EXPIRED");
                    log.info("用户网易云账号过期");
                    Db.updateById(byId);
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
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

}
