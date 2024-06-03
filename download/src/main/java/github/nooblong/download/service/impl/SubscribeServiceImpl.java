package github.nooblong.download.service.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.bilibili.BilibiliBatchIteratorFactory;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.SubscribeTypeEnum;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
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

/**
 * @author lyl
 * @description 针对表【subscribe】的数据库操作Service实现
 * @createDate 2023-10-13 11:19:19
 */
@Service
@Slf4j
public class SubscribeServiceImpl extends ServiceImpl<SubscribeMapper, Subscribe>
        implements SubscribeService {

    private final BilibiliBatchIteratorFactory factory;
    final UploadDetailService uploadDetailService;
    final BilibiliClient bilibiliClient;


    public SubscribeServiceImpl(BilibiliBatchIteratorFactory factory,
                                UploadDetailService uploadDetailService,
                                BilibiliClient bilibiliClient) {
        this.factory = factory;
        this.uploadDetailService = uploadDetailService;
        this.bilibiliClient = bilibiliClient;
    }

    @Async
    @Override
    public void checkAndSave() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1).list();
        for (Subscribe subscribe : subscribeList) {
            try {
                log.info("处理订阅: {}, id: {}, 类型: {}, targetId: {}", subscribe.getRemark(), subscribe.getId(),
                        subscribe.getType(), subscribe.getTargetId());
                Iterator<SimpleVideoInfo> iterator = switch (subscribe.getType()) {
                    case UP -> factory.createUpIterator(subscribe.getTargetId(), subscribe.getKeyWord(),
                            subscribe.getLimitSec(), subscribe.getCheckPart() == 1,
                            VideoOrder.valueOf(subscribe.getVideoOrder()), UserVideoOrder.PUBDATE, availableBilibiliCookie);
                    case COLLECTION -> factory.createCollectionIterator(subscribe.getTargetId(),
                            subscribe.getLimitSec(),
                            VideoOrder.valueOf(subscribe.getVideoOrder()),
                            CollectionVideoOrder.CHANGE, availableBilibiliCookie);
                    case FAVORITE -> factory.createFavoriteIterator(subscribe.getTargetId(),
                            VideoOrder.valueOf(subscribe.getVideoOrder()),
                            subscribe.getLimitSec(), subscribe.getCheckPart() == 1, availableBilibiliCookie);
                    case PART -> factory.createPartIterator(subscribe.getTargetId(),
                            VideoOrder.valueOf(subscribe.getVideoOrder()),
                            subscribe.getLimitSec(), availableBilibiliCookie);
                };

                boolean isProcess = false;
                int processNum = 0;
                while (iterator.hasNext()) {
                    SimpleVideoInfo next = iterator.next();
                    if (next.getCreateTime() != null &&
                            // 倒序就全部遍历，没办法看时间
                            subscribe.getVideoOrder().equals(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name())
                            && DateUtil.compare(new Date(next.getCreateTime() * 1000), subscribe.getProcessTime()) < 0) {
                        log.info("遍历达到处理时间: {}", DateUtil.formatDateTime(subscribe.getProcessTime()));
                        break;
                    }
                    // 判断上传时间区间
                    if (next.getCreateTime() != null &&
                            !DateUtil.isIn(new Date(next.getCreateTime() * 1000), subscribe.getFromTime(), subscribe.getToTime())) {
                        log.info("跳过非区间: {}", DateUtil.formatDateTime(
                                new Date(next.getCreateTime() * 1000)));
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
                    Db.save(uploadDetail);
                    isProcess = true;
                    processNum++;
                }
                if (isProcess) {
                    subscribe.setProcessTime(new Date());
                    log.info("订阅检测完成,发布{}个新视频,时间: {}", processNum, DateUtil.formatDateTime(new Date()));
                    subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 订阅检测完成,发布" + processNum
                            + "个新视频" + "\n");
                    if (subscribe.getType() == SubscribeTypeEnum.PART) {
                        subscribe.setEnable(0);
                    }
                    // 只有第一次是从老到新
                    subscribe.setVideoOrder(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name());
                    updateById(subscribe);
                } else {
                    log.info("未检测到新视频: {}", DateUtil.now());
                    subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 未检测到新视频 " + "\n");
                    updateById(subscribe);
                }
            } catch (Exception e) {
                log.error("订阅: {} 处理失败: {}", subscribe.getId(), e.getMessage());
                subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 订阅处理失败，原因: " + e.getMessage() + "\n");
                updateById(subscribe);
            }
        }
    }

}
