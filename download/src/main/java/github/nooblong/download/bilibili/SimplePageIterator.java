package github.nooblong.download.bilibili;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.VideoOrder;
import github.nooblong.common.util.Constant;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class SimplePageIterator implements Iterator<SimpleVideoInfo> {

    BilibiliClient bilibiliClient;
    int limitSec;
    VideoOrder videoOrder;
    SimpleVideoInfo[] videos;
    int index;
    int totalIndex;// 当total为30时，也就是第31个视频，第二页的开始，30/pageSize为1
    int upVideosTotalNum;
    int pageSize = Constant.SEARCH_PAGE_SIZE;
    private final boolean checkPart;
    List<SimpleVideoInfo> insidePartList = new ArrayList<>();
    Map<String, String> bilibiliCookie;
    String channelIds;
    AtomicInteger counter;

    public SimplePageIterator(BilibiliClient bilibiliClient, int limitSec, VideoOrder videoOrder
            , boolean checkPart, Map<String, String> bilibiliCookie, Integer lastTotalIndex, String channelIds,
                              AtomicInteger counter) {
        this.bilibiliClient = bilibiliClient;
        this.limitSec = limitSec;
        this.videoOrder = videoOrder;
        this.checkPart = checkPart;
        this.bilibiliCookie = bilibiliCookie;
        if (lastTotalIndex > 0) {
            this.totalIndex = lastTotalIndex;
        }
        this.channelIds = channelIds;
        this.counter = counter;
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        if (!insidePartList.isEmpty()) {
            return true;
        }
        if (totalIndex + 1 > upVideosTotalNum) {
            // 就这么多视频
            return false;
        }
        if (index == videos.length) {
            // 检查下一页/上一页
            int currentPn;
            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                // 当前页数
                currentPn = (int) NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP);
                log.info("simple当前第{}页已遍历完，查找下一页:{}", currentPn, currentPn + 1);
                // 检查下一页
                videos = getNextPage(currentPn, pageSize);
            } else {
                currentPn = ((int) NumberUtil.div(upVideosTotalNum, pageSize, 0, RoundingMode.UP) -
                        (int) (NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP))) + 1;
                log.info("simple当前第{}页已遍历完，查找上一页:{}", currentPn, currentPn - 1);
                // 检查上一页
                videos = getPreviousPage(currentPn, pageSize);
            }
            index = 0;
            return true;
        }
        return index < videos.length;
    }

    abstract SimpleVideoInfo[] getNextPage(int currentPn, int pageSize);

    abstract SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize);

    @Override
    public SimpleVideoInfo next() {
        counter.getAndIncrement();
        if (!insidePartList.isEmpty()) {
            SimpleVideoInfo remove = insidePartList.remove(0);
            log.info("simple当前位置:多part内部: {}, cid: {}", remove.getTitle(), remove.getCid());
            return remove;
        }
        int currentRealPageNo = videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ?
                (int) NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP)
                : ((int) NumberUtil.div(upVideosTotalNum, pageSize, 0, RoundingMode.UP) -
                (int) (NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP))) + 1;
        log.info("simple当前位置: {}, 第{}页, 总数:{}, 总位置:{}", index, currentRealPageNo, upVideosTotalNum, totalIndex);
        SimpleVideoInfo result;
        if (hasNext()) {
            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                result = videos[index];
            } else {
                // 倒序遍历
                result = videos[videos.length - 1 - index];
            }
            index++;
            totalIndex++;
            if (result.getDuration() > limitSec && !checkPart) {
                log.info("simple歌曲:{} 时长:{} 超过了限制:{}", result.getTitle(), result.getDuration(), limitSec);
                return next();
            }
            if (checkPart || StrUtil.isNotBlank(channelIds)) {
                BilibiliFullVideo fullVideo = bilibiliClient.getFullVideo(result.getBvid(), bilibiliCookie);

                if (StrUtil.isNotBlank(channelIds)) {
                    // todo: 待测试 判断合集
                    // 判断是否在合集里
                    List<String> channelIdList = CommonUtil.toList(channelIds);
                    String seasonId = fullVideo.getSeasonId();
                    if (!(seasonId != null && channelIdList.contains(seasonId))) {
                        log.info("simple歌曲:{} 合集: {} 不属于合集: {}", result.getTitle(), seasonId, channelIds);
                        return next();
                    }
                }

                if (checkPart) {
                    // 判断是否包含多p
                    if (fullVideo.getHasMultiPart()) {
                        log.info("simple检测到多p视频: {}", fullVideo.getTitle());
                        // 多p视频不要直接返回，从p1开始返回
                        // 对part内做时间限制
                        try {

                            Iterator<SimpleVideoInfo> partIterator =
                                    new PartIterator(bilibiliClient, limitSec, VideoOrder.PUB_NEW_FIRST_THEN_OLD,
                                            fullVideo.getBvid(), bilibiliCookie);
                            while (partIterator.hasNext()) {
                                SimpleVideoInfo next = partIterator.next();
                                // 将视频的createTime赋值
                                next.setCreateTime(result.getCreateTime());
                                insidePartList.add(next);
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            // ignore
                        }
                        SimpleVideoInfo remove = insidePartList.remove(0);
                        log.info("simple多p视频第一次返回: {}", remove.getPartName());
                        return remove;
                    }
                }
            }
            // 不是多p
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public abstract void lazyInit();

}
