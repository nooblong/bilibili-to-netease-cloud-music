package github.nooblong.btncm.service.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.utils.CommonUtil;
import github.nooblong.btncm.enums.SubscribeTypeEnum;
import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.bilibili.FavoriteIterator;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.bilibili.UpIterator;
import github.nooblong.btncm.enums.UserVideoOrderEnum;
import github.nooblong.btncm.enums.VideoOrderEnum;
import github.nooblong.btncm.entity.Subscribe;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.mapper.SubscribeMapper;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
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
    final NetMusicClient netMusicClient;


    public SubscribeServiceImpl(UploadDetailService uploadDetailService,
                                BilibiliClient bilibiliClient,
                                NetMusicClient netMusicClient) {
        this.uploadDetailService = uploadDetailService;
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
    }

    @Async
    @Override
    public void checkAndSave() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getBilibiliCookie();
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1).list();
        for (Subscribe subscribe : subscribeList) {
            checkSubscribe(subscribe, availableBilibiliCookie);
        }
    }

    @Override
    public void checkAndSave(Long userId, Long voiceListId) {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getBilibiliCookie();
        List<Subscribe> subscribeList = lambdaQuery().eq(Subscribe::getEnable, 1)
                .eq(Subscribe::getVoiceListId, voiceListId)
                .eq(Subscribe::getUserId, userId).list();
        for (Subscribe subscribe : subscribeList) {
            checkSubscribe(subscribe, availableBilibiliCookie);
        }
    }

    @Override
    public void syncUpNameAndImage() {
        List<Subscribe> subscribes = list();
        for (Subscribe subscribe : subscribes) {
            String upId = subscribe.getUpId();
            if (StrUtil.isNotBlank(upId) && StrUtil.isBlank(subscribe.getUpName())) {
                try {
                    JsonNode userInfo = bilibiliClient.getUserInfo(upId, new HashMap<>());
                    subscribe.setUpImage(userInfo.get("data").get("face").asText());
                    subscribe.setUpName(userInfo.get("data").get("name").asText());
                    Db.updateById(subscribe);
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("err: ", e);
                }
            }
        }
    }

    public void checkSubscribe(Subscribe subscribe, Map<String, String> availableBilibiliCookie) {
        Long userId = subscribe.getUserId();
        if (userId == null) {
            return;
        }
        SysUser user = Db.getById(userId, SysUser.class);
        if (StrUtil.isBlank(user.getNetCookies())) {
            log.info("用户未登录不处理，订阅:{} 用户:{}", subscribe.getId(), user.getId());
            subscribe.setLog("用户未登录不处理\n");
            updateById(subscribe);
            return;
        }
        AtomicInteger counter;
        if (subscribe.getLastTotalIndex() < 0) {
            counter = new AtomicInteger(1);
        } else {
            counter = new AtomicInteger(subscribe.getLastTotalIndex());
        }
        try {
            if (subscribe.getType() == SubscribeTypeEnum.UP) {
                UpIterator upIterator = new UpIterator(bilibiliClient, subscribe.getUpId(), subscribe.getKeyWord(),
                        subscribe.getLimitSec(), subscribe.getMinSec(), VideoOrderEnum.valueOf(subscribe.getVideoOrder()),
                        UserVideoOrderEnum.PUBDATE, subscribe.getCheckPart() == 1,
                        availableBilibiliCookie, subscribe.getLastTotalIndex(), subscribe.getChannelIds(), counter);
                process(subscribe, upIterator);
                subscribe.setProcessTime(new Date());
                subscribe.setLastTotalIndex(-1);
                if (subscribe.getPriority() <= 0) {
                    subscribe.setPriority(1);
                }
                updateById(subscribe);
            }
            if (subscribe.getType() == SubscribeTypeEnum.FAVORITE) {
                String favIds = subscribe.getChannelIds();
                List<String> favIdList = CommonUtil.toList(favIds);
                for (String favId : favIdList) {
                    FavoriteIterator favIterator = new FavoriteIterator(favId, bilibiliClient,
                            subscribe.getLimitSec(), subscribe.getMinSec(), subscribe.getCheckPart() == 1,
                            availableBilibiliCookie);
                    process(subscribe, favIterator);
                }
                subscribe.setProcessTime(new Date());
                subscribe.setLastTotalIndex(-1);
                if (subscribe.getPriority() <= 0) {
                    subscribe.setPriority(1);
                }
                updateById(subscribe);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            if (counter.get() > 1) {
                log.error("订阅: {}处理失败, 但是遍历到了第{}页, 下次将从此开始", subscribe.getId(), counter.get());
                subscribe.setLastTotalIndex(counter.get());
                subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + "已经遍历到了第" + counter.get() + "页\n");
            }
            log.error("订阅: {} 处理失败: {}", subscribe.getId(), e.getMessage());
            log.error(e.getMessage(), e);
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() +
                    " 订阅处理失败，原因: " + e.getMessage() + "\n");
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
                    subscribe.getVideoOrder().equals(VideoOrderEnum.PUB_NEW_FIRST_THEN_OLD.name())
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
                    .setBitrate(subscribe.getBitrate())
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
            subscribe.setVideoOrder(VideoOrderEnum.PUB_NEW_FIRST_THEN_OLD.name());
        } else {
            log.info("未检测到新视频: {}", DateUtil.now());
            subscribe.setLog(CommonUtil.processString(subscribe.getLog()) + DateUtil.now() + " 未检测到新视频 " + "\n");
        }
    }

    @Override
    public List<UploadDetail> testProcess(Subscribe subscribe, Iterator<SimpleVideoInfo> iterator, int times) {
        List<UploadDetail> result = new ArrayList<>();
        while (iterator.hasNext()) {
            SimpleVideoInfo next = iterator.next();
            if (times-- <= 0) {
                break;
            }

            // 判断from-to区间
            if (next.getCreateTime() != null &&
                    !DateUtil.isIn(new Date(next.getCreateTime() * 1000), subscribe.getFromTime(),
                            subscribe.getToTime())) {
                continue;
            }
            // 判断关键词跳过
            if (StrUtil.isNotBlank(subscribe.getKeyWord()) &&
                    StrUtil.isNotBlank(next.getTitle()) &&
                    !next.getTitle().contains(subscribe.getKeyWord())) {
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
            result.add(uploadDetail);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void removeUselessCookie() {
        List<SysUser> userList = SimpleQuery.list(Wrappers.lambdaQuery(SysUser.class), i -> i);
        for (SysUser user : userList) {
            if (StrUtil.isNotBlank(user.getNetCookies())) {
                JsonNode loginStatus = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginStatus", user.getId());
                if (loginStatus.get("account") != null
                && loginStatus.get("account").get("id") != null
                && !loginStatus.get("account").get("id").asText().isEmpty()) {
                    log.info("用户{}网易登录有效", user.getUsername());
                } else {
                    log.info("清除网易云cookie: 用户{}", user.getUsername());
                    user.setNetCookies("");
                    Db.updateById(user);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

}
