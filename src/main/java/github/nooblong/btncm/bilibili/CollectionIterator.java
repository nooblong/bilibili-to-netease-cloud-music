package github.nooblong.btncm.bilibili;

import github.nooblong.btncm.enums.CollectionVideoOrderEnum;
import github.nooblong.btncm.enums.VideoOrderEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 视频合集迭代器
 */
@Slf4j
public class CollectionIterator extends SimplePageIterator {

    String collectionId;
    CollectionVideoOrderEnum collectionVideoOrder;

    public CollectionIterator(BilibiliClient bilibiliClient,
                              int limitSec,
                              int minSec,
                              VideoOrderEnum videoOrder, String collectionId, CollectionVideoOrderEnum collectionVideoOrder,
                              Map<String, String> bilibiliCookie, Integer lastTotalIndex, String channelIds,
                              AtomicInteger counter) {
        super(bilibiliClient, limitSec, minSec, videoOrder, false, bilibiliCookie, lastTotalIndex, channelIds, counter);
        this.collectionId = collectionId;
        this.collectionVideoOrder = collectionVideoOrder;
    }

    @Override
    SimpleVideoInfo[] getNextPage(int currentPn, int pageSize) {
        return bilibiliClient.getCollectionVideoListFromBilibili(
                collectionId,
                pageSize,
                currentPn + 1,
                collectionVideoOrder,
                bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize) {
        return bilibiliClient.getCollectionVideoListFromBilibili(
                collectionId,
                pageSize,
                currentPn - 1,
                collectionVideoOrder,
                bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    public void lazyInit() {
        if (this.videos == null) {
            // 第一次初始化
            // 先查总数
            log.debug("初始化CollectionIterator:");
            if (videoOrder == VideoOrderEnum.PUB_NEW_FIRST_THEN_OLD) {
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili = bilibiliClient.getCollectionVideoListFromBilibili(
                        collectionId,
                        pageSize,
                        1,
                        collectionVideoOrder,
                        bilibiliCookie);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            } else {
                if (upVideosTotalNum == 0) {
                    IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili = bilibiliClient.getCollectionVideoListFromBilibili(
                            collectionId,
                            pageSize,
                            1,
                            collectionVideoOrder,
                            bilibiliCookie);
                    log.debug("collection先获取一遍总数: {}", collectionVideoListFromBilibili.getTotalNum());
                    upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
                }
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili = bilibiliClient.getCollectionVideoListFromBilibili(
                        collectionId,
                        pageSize,
                        videoOrder == VideoOrderEnum.PUB_NEW_FIRST_THEN_OLD ? 1 : (upVideosTotalNum / pageSize) + 1,
                        collectionVideoOrder,
                        bilibiliCookie);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            }
        }
    }
}
