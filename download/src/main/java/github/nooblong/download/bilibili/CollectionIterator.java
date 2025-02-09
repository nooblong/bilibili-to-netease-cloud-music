package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CollectionIterator extends SimplePageIterator {

    String collectionId;
    CollectionVideoOrder collectionVideoOrder;

    public CollectionIterator(BilibiliClient bilibiliClient, int limitSec,
                              VideoOrder videoOrder, String collectionId, CollectionVideoOrder collectionVideoOrder,
                              Map<String, String> bilibiliCookie, Integer lastTotalIndex, String channelIds,
                              AtomicInteger counter) {
        super(bilibiliClient, limitSec, videoOrder, false, bilibiliCookie, lastTotalIndex, channelIds, counter);
        this.collectionId = collectionId;
        this.collectionVideoOrder = collectionVideoOrder;
    }

    @Override
    SimpleVideoInfo[] getNextPage(int currentPn, int pageSize) {
        return bilibiliClient.getCollectionVideoListFromBilibili(collectionId, pageSize, currentPn + 1,
                collectionVideoOrder, bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize) {
        return bilibiliClient.getCollectionVideoListFromBilibili(collectionId, pageSize, currentPn - 1,
                collectionVideoOrder, bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    public void lazyInit() {
        if (this.videos == null) {
            // 第一次初始化
            // 先查总数
            log.info("初始化集合:");


            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                        bilibiliClient.getCollectionVideoListFromBilibili(collectionId, pageSize, 1, collectionVideoOrder,
                                bilibiliCookie);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            } else {
                if (upVideosTotalNum == 0) {
                    IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                            bilibiliClient.getCollectionVideoListFromBilibili(collectionId, pageSize, 1, collectionVideoOrder,
                                    bilibiliCookie);
                    log.info("collection先获取一遍总数: {}", collectionVideoListFromBilibili.getTotalNum());
                    upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
                }
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                        bilibiliClient.getCollectionVideoListFromBilibili(collectionId, pageSize,
                                videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ? 1 : (upVideosTotalNum / pageSize) + 1,
                                collectionVideoOrder, bilibiliCookie);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            }
        }
    }
}
