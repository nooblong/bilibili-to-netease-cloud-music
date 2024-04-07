package github.nooblong.download.bilibili;

import cn.hutool.core.util.NumberUtil;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.utils.Constant;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public abstract class SimplePageIterator implements Iterator<SimpleVideoInfo> {

    BilibiliBatchIteratorFactory factory;
    int limitSec;
    VideoOrder videoOrder;
    SimpleVideoInfo[] videos;
    int index;
    int totalIndex;// 当total为30时，也就是第31个视频，第二页的开始，30/pageSize为1
    int upVideosTotalNum;
    int pageSize = Constant.SEARCH_PAGE_SIZE;
    private final boolean checkPart;
    List<SimpleVideoInfo> insidePartList = new ArrayList<>();

    public SimplePageIterator(BilibiliBatchIteratorFactory factory, int limitSec, VideoOrder videoOrder, boolean checkPart) {
        this.factory = factory;
        this.limitSec = limitSec;
        this.videoOrder = videoOrder;
        this.checkPart = checkPart;
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
                log.info("当前第{}页已遍历完，查找下一页:{}", currentPn, currentPn + 1);
                // 检查下一页
                videos = getNextPage(currentPn, pageSize);
            } else {
                currentPn = ((int) NumberUtil.div(upVideosTotalNum, pageSize, 0, RoundingMode.UP) -
                        (int) (NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP))) + 1;
                log.info("当前第{}页已遍历完，查找上一页:{}", currentPn, currentPn - 1);
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
        if (!insidePartList.isEmpty()) {
            SimpleVideoInfo remove = insidePartList.remove(0);
            log.info("当前位置:多part内部: {}, cid: {}", remove.getTitle(), remove.getCid());
            return remove;
        }
        int currentRealPageNo = videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ?
                (int) NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP)
                : ((int) NumberUtil.div(upVideosTotalNum, pageSize, 0, RoundingMode.UP) -
                (int) (NumberUtil.div(totalIndex, pageSize, 0, RoundingMode.UP))) + 1;
        log.info("当前位置: {}, 第{}页, 总数:{}, 总位置:{}", index, currentRealPageNo, upVideosTotalNum, totalIndex);
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
                log.info("歌曲:{} 时长:{} 超过了限制:{}", result.getTitle(), result.getDuration(), limitSec);
                return next();
            }
            if (checkPart) {
                BilibiliFullVideo fullVideo = factory.getFullVideo(result.getBvid());
                if (fullVideo.getHasMultiPart()) {
                    // 多p视频不要直接返回，从p1开始返回
                    // 对part内做时间限制
                    Iterator<SimpleVideoInfo> partIterator =
                            factory.createPartIterator(fullVideo.getBvid(), VideoOrder.PUB_NEW_FIRST_THEN_OLD, limitSec);
                    
                }
            }
            // 不是多p
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public abstract void lazyInit();

}
