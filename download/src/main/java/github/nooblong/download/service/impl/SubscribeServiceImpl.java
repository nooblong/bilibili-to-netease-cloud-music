package github.nooblong.download.service.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.download.SubscribeTypeEnum;
import github.nooblong.download.bilibili.BilibiliBatchIteratorFactory;
import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.SubscribeReg;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mapper.SubscribeMapper;
import github.nooblong.download.mq.MessageSender;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

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
    final MessageSender messageSender;
    final UploadDetailService uploadDetailService;


    public SubscribeServiceImpl(BilibiliBatchIteratorFactory factory,
                                MessageSender messageSender,
                                UploadDetailService uploadDetailService) {
        this.factory = factory;
        this.messageSender = messageSender;
        this.uploadDetailService = uploadDetailService;
    }

    @Override
    public void checkAndSendMessage() {
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1).list();
        for (Subscribe subscribe : subscribeList) {
            log.info("处理订阅: {}, id: {}, 类型: {}, targetId: {}", subscribe.getRemark(), subscribe.getId(),
                    subscribe.getType(), subscribe.getTargetId());
            Iterator<BilibiliVideo> iterator = switch (SubscribeTypeEnum.valueOf(subscribe.getType())) {
                case UP -> factory.createUpIterator(subscribe.getTargetId(), subscribe.getKeyWord(),
                        subscribe.getLimitSec(), VideoOrder.valueOf(subscribe.getVideoOrder()), UserVideoOrder.PUBDATE);
                case COLLECTION -> factory.createCollectionIterator(subscribe.getTargetId(),
                        subscribe.getLimitSec(),
                        VideoOrder.valueOf(subscribe.getVideoOrder()),
                        CollectionVideoOrder.CHANGE);
                case FAVORITE -> factory.createFavoriteIterator(subscribe.getTargetId(),
                        VideoOrder.valueOf(subscribe.getVideoOrder()),
                        subscribe.getLimitSec());
                case PART -> factory.createPartIterator(subscribe.getTargetId(),
                        VideoOrder.valueOf(subscribe.getVideoOrder()),
                        subscribe.getLimitSec());
            };

            boolean isProcess = false;

            while (iterator.hasNext()) {
                BilibiliVideo next = iterator.next();
                if (next.getVideoCreateTime() != null &&
                        // 倒序就全部遍历，没办法看时间
                        subscribe.getVideoOrder().equals(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name())
                        && DateUtil.compare(next.getVideoCreateTime(), subscribe.getProcessTime()) < 0) {
                    log.info("遍历达到处理时间: {}", DateUtil.formatDateTime(subscribe.getProcessTime()));
                    break;
                }
                // 判断上传时间区间
                if (next.getVideoCreateTime() != null &&
                        !DateUtil.isIn(next.getVideoCreateTime(), subscribe.getFromTime(), subscribe.getToTime())) {
                    log.info("跳过非区间: {}", DateUtil.formatDateTime(next.getVideoCreateTime()));
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
                        .setUseVideoCover(subscribe.getUseVideoCover() == 0 ? 1L : 0L)
                        .setVoiceListId(subscribe.getVoiceListId())
                        .setUserId(subscribe.getUserId());
                // 根据subscribe设置uploadName
                String regName = subscribe.getRegName();
                if (regName != null) {
                    LambdaQueryWrapper<SubscribeReg> eq = Wrappers.lambdaQuery(SubscribeReg.class).eq(SubscribeReg::getSubscribeId, subscribe.getId());
                    List<SubscribeReg> subscribeRegs = Db.list(eq);
                    Map<Integer, String> replaceMap = new HashMap<>();
                    for (SubscribeReg subscribeReg : subscribeRegs) {
                        // 先读取了原标题的reg生成map
                        try {
                            String s1 = ReUtil.extractMulti(subscribeReg.getRegex(), next.getTitle(), "$1");
                            if (s1 != null) {
                                replaceMap.put(subscribeReg.getPos(), s1);
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }
                    // 利用map替换subscribe的Name
                    String result = ReUtil.replaceAll(regName, "\\{(.*?)}", match -> {
                        String content = match.group(0);
                        content = content.substring(1, content.length() - 1);
                        if (content.equals("pubdate")) {
                            Date videoCreateTime = next.getVideoCreateTime();
                            return DateUtil.format(videoCreateTime, "yyyy.MM.dd");
                        } else if (NumberUtil.isNumber(content)) {
                            if (replaceMap.containsKey(Integer.valueOf(content))) {
                                return replaceMap.getOrDefault(Integer.valueOf(content), "");
                            }
                        } else {
                            return content;
                        }
                        return content;
                    });
                    uploadDetail.setUploadName(result);
                }
                log.info("上传名字: {}, bvid: {}, date: {}",
                        uploadDetail.getUploadName(), uploadDetail.getBvid(),
                        DateUtil.format(next.getVideoCreateTime(), DatePattern.NORM_DATE_PATTERN));
                Db.save(uploadDetail);
                Assert.notNull(uploadDetail.getId(), "上传->保存数据库失败");

                log.info("上传" + next.getTitle());
                messageSender.sendUploadDetailId(uploadDetail.getId(), subscribe.getPriority());
                isProcess = true;
            }
            if (isProcess) {
                subscribe.setProcessTime(new Date());
                log.info("更新订阅处理时间: {}", DateUtil.formatDateTime(new Date()));
                if (subscribe.getType().equals(SubscribeTypeEnum.PART.name())) {
                    subscribe.setEnable(0);
                    // 只有第一次是从老到新
                }
                subscribe.setVideoOrder(VideoOrder.PUB_NEW_FIRST_THEN_OLD.name());
                updateById(subscribe);
            } else {
                log.info("未处理，无需更新时间");
            }
        }
    }

}
