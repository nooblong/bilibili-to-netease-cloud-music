package github.nooblong.download.service.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.SubscribeTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.FavoriteIterator;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.bilibili.UpIterator;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.VideoOrder;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mapper.SubscribeMapper;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lyl
 * @description 针对表【subscribe】的数据库操作Service实现
 * @createDate 2023-10-13 11:19:19
 */
@Service
@Slf4j
public class SubscribeServiceImpl extends ServiceImpl<SubscribeMapper, Subscribe>
        implements SubscribeService {

    final UploadDetailService uploadDetailService;
    final BilibiliClient bilibiliClient;


    public SubscribeServiceImpl(UploadDetailService uploadDetailService,
                                BilibiliClient bilibiliClient) {
        this.uploadDetailService = uploadDetailService;
        this.bilibiliClient = bilibiliClient;
    }

    @Async
    @Override
    public void checkAndSave() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1).list();
        for (Subscribe subscribe : subscribeList) {
            checkSubscribe(subscribe, availableBilibiliCookie);
        }
    }

    @Override
    public void checkAndSave(Long userId) {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1)
                .eq(Subscribe::getUserId, userId).list();
        for (Subscribe subscribe : subscribeList) {
            checkSubscribe(subscribe, availableBilibiliCookie);
        }
    }

    private void checkSubscribe(Subscribe subscribe, Map<String, String> availableBilibiliCookie) {
        AtomicInteger counter = new AtomicInteger(0);
        try {
            if (subscribe.getType() == SubscribeTypeEnum.UP) {
                UpIterator upIterator = new UpIterator(bilibiliClient, subscribe.getUpId(), subscribe.getKeyWord(),
                        subscribe.getLimitSec(), VideoOrder.valueOf(subscribe.getVideoOrder()),
                        UserVideoOrder.PUBDATE, subscribe.getCheckPart() == 1,
                        availableBilibiliCookie, subscribe.getLastTotalIndex(), subscribe.getChannelIds(), counter);
                process(subscribe, upIterator);
                subscribe.setProcessTime(new Date());
                updateById(subscribe);
            }
            if (subscribe.getType() == SubscribeTypeEnum.FAVORITE) {
                String favIds = subscribe.getChannelIds();
                List<String> favIdList = CommonUtil.toList(favIds);
                for (String favId : favIdList) {
                    FavoriteIterator favIterator = new FavoriteIterator(favId, bilibiliClient,
                            subscribe.getLimitSec(), subscribe.getCheckPart() == 1,
                            availableBilibiliCookie);
                    process(subscribe, favIterator);
                }
                subscribe.setProcessTime(new Date());
                updateById(subscribe);
            }
        } catch (Exception e) {
            // todo: 待测试 last total index
            if (counter.get() > 0) {
                log.error("订阅: {}处理失败, 但是遍历到了{}, 下次将从此开始", subscribe.getId(), counter.get());
                subscribe.setLastTotalIndex(counter.get());
            }
            log.error("订阅: {} 处理失败: {}", subscribe.getId(), e.getMessage());
            log.error(e.getMessage(), e);
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() +
                    " 订阅处理失败，原因: " + CommonUtil.limitString(e.getMessage()) + "\n");
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + "已经遍历到了:" + counter.get() + "\n");
            updateById(subscribe);
        }
    }

    private void process(Subscribe subscribe, Iterator<SimpleVideoInfo> iterator) {
        boolean isProcess = false;
        int processNum = 0;
        while (iterator.hasNext()) {
            SimpleVideoInfo next = iterator.next();

            if (next.getCreateTime() != null &&
                    // 倒序就全部遍历，没办法看时间
                    subscribe.getVideoOrder().equals(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name())
                    && DateUtil.compare(new Date(next.getCreateTime() * 1000), subscribe.getProcessTime()) < 0) {
                log.info("检测到视频: {}, 遍历达到处理时间: {}", next.getTitle(),
                        DateUtil.formatDateTime(subscribe.getProcessTime()));
                break;
            }

            // 判断from-to区间
            if (next.getCreateTime() != null &&
                    !DateUtil.isIn(new Date(next.getCreateTime() * 1000), subscribe.getFromTime(),
                            subscribe.getToTime())) {
                log.info("跳过非区间: {}", DateUtil.formatDateTime(
                        new Date(next.getCreateTime() * 1000)));
                continue;
            }

            // 判断关键词跳过
            if (StrUtil.isNotBlank(subscribe.getKeyWord()) &&
                    StrUtil.isNotBlank(next.getTitle()) &&
                    !next.getTitle().contains(subscribe.getKeyWord())) {
                log.info("跳过关键词: {}", subscribe.getKeyWord());
                continue;
            }

            // 查重
            boolean unique = uploadDetailService.isUnique(next.getBvid(),
                    next.getCid() == null ? "" : next.getCid(),
                    subscribe.getVoiceListId());
            if (!unique) {
                log.info("歌曲已上传: {}, {}", next.getTitle(), next.getPartName());
                continue;
            }

            UploadDetail uploadDetail = new UploadDetail();
            uploadDetail.setBvid(next.getBvid())
                    .setCid(next.getCid())
                    .setSubscribeId(subscribe.getId())
                    .setTitle(next.getTitle())
                    .setCrack(subscribe.getCrack().longValue())
                    .setUseVideoCover(subscribe.getUseVideoCover().longValue())
                    .setVoiceListId(subscribe.getVoiceListId())
                    .setPriority(subscribe.getPriority().longValue())
                    .setUserId(subscribe.getUserId());
            if (next.getCreateTime() != null) {
                log.info("保存: {}, bvid: {}, date: {}",
                        uploadDetail.getTitle(), uploadDetail.getBvid(),
                        DateUtil.format(new Date(next.getCreateTime() * 1000), DatePattern.NORM_DATE_PATTERN));
            } else {
                log.info("保存(无视频创建时间，应该为多p视频): {}, bvid: {}",
                        uploadDetail.getTitle(), uploadDetail.getBvid());
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Db.save(uploadDetail);
            isProcess = true;
            processNum++;
        }
        if (isProcess) {
            log.info("订阅检测完成,发布{}个新视频,时间: {}", processNum, DateUtil.formatDateTime(new Date()));
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 订阅检测完成,发布" + processNum
                    + "个新视频" + "\n");
            // 只有第一次是从老到新
            subscribe.setVideoOrder(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name());
        } else {
            log.info("未检测到新视频: {}", DateUtil.now());
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 未检测到新视频 " + "\n");
        }
    }

}
