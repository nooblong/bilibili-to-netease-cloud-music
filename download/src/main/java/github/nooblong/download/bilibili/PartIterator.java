package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;

@Slf4j
public class PartIterator implements Iterator<SimpleVideoInfo> {

    BilibiliBatchIteratorFactory factory;
    int limitSec;
    VideoOrder videoOrder;
    SimpleVideoInfo[] videos;
    int index;
    int upVideosTotalNum;
    String bvid;
    Map<String, String> bilibiliCookie;

    public PartIterator(BilibiliBatchIteratorFactory factory, int limitSec, VideoOrder videoOrder, String bvid,
                        Map<String, String> bilibiliCookie) {
        this.factory = factory;
        this.limitSec = limitSec;
        this.videoOrder = videoOrder;
        this.bvid = bvid;
        this.bilibiliCookie = bilibiliCookie;
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        return index < videos.length;
    }

    @Override
    public SimpleVideoInfo next() {
        SimpleVideoInfo result;
        if (hasNext()) {
            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                result = videos[index];
            } else {
                // 倒序遍历
                result = videos[videos.length - 1 - index];
            }
            index++;
            if (result.getDuration() > limitSec) {
                log.info("part超限: 歌曲:{} 时长:{} 超过了限制:{}", result.getPartName(), result.getDuration(), limitSec);
                return next();
            }
            log.info("part: {}, 位置: {}, 总数:{}", result.getPartName(), index, upVideosTotalNum);
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public void lazyInit() {
        if (this.videos == null) {
            log.info("初始化集合:");
            // 第一次初始化
            // 先查总数
            IteratorCollectionTotalList<SimpleVideoInfo> partVideosFromBilibili = factory.getPartVideosFromBilibili(bvid, bilibiliCookie);
            videos = partVideosFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
            upVideosTotalNum = partVideosFromBilibili.getTotalNum();
        }
    }
}
