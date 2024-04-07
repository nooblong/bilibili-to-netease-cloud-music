package github.nooblong.download.bilibili;

import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

@Slf4j
public class FavoriteIterator implements Iterator<SimpleVideoInfo> {

    private final String favoriteId;
    private final BilibiliBatchIteratorFactory factory;
    private int limitSec;
    private int index;
    private SimpleVideoInfo[] videos;
    private int page = 1;
    private int hasNextPage;
    private final boolean checkPart;

    public FavoriteIterator(String favoriteId, BilibiliBatchIteratorFactory factory, int limitSec, boolean checkPart) {
        this.favoriteId = favoriteId;
        this.factory = factory;
        this.limitSec = limitSec;
        this.checkPart = checkPart;
    }

    public void lazyInit() {
        if (videos == null) {
            log.info("favorite初始化集合:");
            IteratorCollectionTotalList<SimpleVideoInfo> favoriteVideoListFromBilibili = factory.getFavoriteVideoListFromBilibili(favoriteId, page);
            videos = favoriteVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
            hasNextPage = favoriteVideoListFromBilibili.getTotalNum();
        }
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        if (index == videos.length && hasNextPage != 0) {
            log.info("favorite收藏夹还有下一页");
            IteratorCollectionTotalList<SimpleVideoInfo> favoriteVideoListFromBilibili = factory.getFavoriteVideoListFromBilibili(favoriteId, ++page);
            videos = favoriteVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
            hasNextPage = favoriteVideoListFromBilibili.getTotalNum();
            index = 0;
            return true;
        }
        return index < videos.length;
    }

    @Override
    public SimpleVideoInfo next() {
        log.info("favorite当前位置: {}", index);
        SimpleVideoInfo result;
        if (hasNext()) {
            result = videos[index];
            index++;
            if (result.getDuration() > limitSec && !checkPart) {
                log.info("favorite歌曲:{} 时长:{} 超过了限制:{}", result.getTitle(), result.getDuration(), limitSec);
                return next();
            }
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }
}
