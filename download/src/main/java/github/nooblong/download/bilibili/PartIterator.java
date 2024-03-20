package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class PartIterator implements Iterator<SimpleVideoInfo> {

    BilibiliBatchIteratorFactory factory;
    int limitSec;
    VideoOrder videoOrder;
    SimpleVideoInfo[] videos;
    int index;
    int upVideosTotalNum;
    String bvid;

    public PartIterator(BilibiliBatchIteratorFactory factory, int limitSec, VideoOrder videoOrder, String bvid) {
        this.factory = factory;
        this.limitSec = limitSec;
        this.videoOrder = videoOrder;
        this.bvid = bvid;
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        return index < videos.length;
    }

    @Override
    public SimpleVideoInfo next() {
        log.info("当前位置: {}, 总数:{}", index, upVideosTotalNum);
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
                log.info("歌曲:{} 时长:{} 超过了限制:{}", result.getPartName(), result.getDuration(), limitSec);
                return next();
            }
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public void lazyInit() {
        if (this.videos == null) {
            log.info("初始化集合:");
            // 第一次初始化
            // 先查总数
            IteratorCollectionTotalList<SimpleVideoInfo> partVideosFromBilibili = factory.getPartVideosFromBilibili(bvid);
            videos = partVideosFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
            upVideosTotalNum = partVideosFromBilibili.getTotalNum();
        }
    }
}
