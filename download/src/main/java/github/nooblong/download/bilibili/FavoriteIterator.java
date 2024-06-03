package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

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
    List<SimpleVideoInfo> insidePartList = new ArrayList<>();
    Map<String, String> bilibiliCookie;

    public FavoriteIterator(String favoriteId, BilibiliBatchIteratorFactory factory, int limitSec,
                            boolean checkPart, Map<String, String> bilibiliCookie) {
        this.favoriteId = favoriteId;
        this.factory = factory;
        this.limitSec = limitSec;
        this.checkPart = checkPart;
        this.bilibiliCookie = bilibiliCookie;
    }

    public void lazyInit() {
        if (videos == null) {
            log.info("favorite初始化集合:");
            IteratorCollectionTotalList<SimpleVideoInfo> favoriteVideoListFromBilibili =
                    factory.getFavoriteVideoListFromBilibili(favoriteId, page, bilibiliCookie);
            videos = favoriteVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
            hasNextPage = favoriteVideoListFromBilibili.getTotalNum();
        }
    }

    @Override
    public boolean hasNext() {
        lazyInit();
        if (index == videos.length && hasNextPage != 0) {
            log.info("favorite收藏夹还有下一页");
            IteratorCollectionTotalList<SimpleVideoInfo> favoriteVideoListFromBilibili =
                    factory.getFavoriteVideoListFromBilibili(favoriteId, ++page, bilibiliCookie);
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
            if (!insidePartList.isEmpty()) {
                SimpleVideoInfo remove = insidePartList.remove(0);
                log.info("simple当前位置:多part内部: {}, cid: {}", remove.getTitle(), remove.getCid());
                return remove;
            }
            result = videos[index];
            index++;
            if (result.getDuration() > limitSec && !checkPart) {
                log.info("favorite歌曲:{} 时长:{} 超过了限制:{}", result.getTitle(), result.getDuration(), limitSec);
                return next();
            }
            if (checkPart) {
                BilibiliFullVideo fullVideo = factory.getFullVideo(result.getBvid(), new HashMap<>());
                if (fullVideo.getHasMultiPart()) {
                    log.info("simple检测到多p视频: {}", fullVideo.getTitle());
                    // 多p视频不要直接返回，从p1开始返回
                    // 对part内做时间限制
                    try {

                        Iterator<SimpleVideoInfo> partIterator = factory.createPartIterator(
                                fullVideo.getBvid(), VideoOrder.PUB_NEW_FIRST_THEN_OLD, limitSec, bilibiliCookie);
                        while (partIterator.hasNext()) {
                            SimpleVideoInfo next = partIterator.next();
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
            return result;
        }
        throw new ArrayIndexOutOfBoundsException();
    }
}
