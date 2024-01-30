package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class FavoriteIterator implements Iterator<BilibiliVideo> {

    private final String favoriteId;
    private final BilibiliBatchIteratorFactory factory;
    private final int limitSec;
    private final VideoOrder videoOrder;// 只能正序
    private int index;
    private BilibiliVideo[] videos;
    private static final int NUM_PER_PAGE = 20;
    private int page = 1;
    private int hasNextPage;

    public FavoriteIterator(String favoriteId, BilibiliBatchIteratorFactory factory, int limitSec, VideoOrder videoOrder) {
        this.favoriteId = favoriteId;
        this.factory = factory;
        this.limitSec = limitSec;
        this.videoOrder = videoOrder;
    }

    public void lazyInit() {
        if (videos == null) {
            log.info("初始化集合:");
            IteratorCollectionTotalList<BilibiliVideo> favoriteVideoListFromBilibili = factory.getFavoriteVideoListFromBilibili(favoriteId, page);
            videos = favoriteVideoListFromBilibili.getData().toArray(new BilibiliVideo[0]);
            hasNextPage = favoriteVideoListFromBilibili.getTotalNum();
        }
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        if (index == videos.length && hasNextPage != 0) {
            log.info("收藏夹还有下一页");
            IteratorCollectionTotalList<BilibiliVideo> favoriteVideoListFromBilibili = factory.getFavoriteVideoListFromBilibili(favoriteId, ++page);
            videos = favoriteVideoListFromBilibili.getData().toArray(new BilibiliVideo[0]);
            hasNextPage = favoriteVideoListFromBilibili.getTotalNum();
            index = 0;
            return true;
        }
        return index < videos.length;
    }

    @Override
    public BilibiliVideo next() {
        log.info("当前位置: {}", index);
        BilibiliVideo result;
        if (hasNext()) {
            result = videos[index];
            index++;
            if (result.getDuration() > limitSec) {
                log.info("歌曲:{} 时长:{} 超过了限制:{}", result.getTitle(), result.getDuration(), limitSec);
                return next();
            }
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }
}
