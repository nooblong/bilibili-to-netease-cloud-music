package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionIterator extends SimplePageIterator {

    String collectionId;
    CollectionVideoOrder collectionVideoOrder;

    public CollectionIterator(BilibiliBatchIteratorFactory factory, int limitSec,
                              VideoOrder videoOrder, String collectionId, CollectionVideoOrder collectionVideoOrder) {
        super(factory, limitSec, videoOrder);
        this.collectionId = collectionId;
        this.collectionVideoOrder = collectionVideoOrder;
    }

    @Override
    SimpleVideoInfo[] getNextPage(int currentPn, int pageSize) {
        return factory.getCollectionVideoListFromBilibili(collectionId, pageSize, currentPn + 1,
                collectionVideoOrder).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize) {
        return factory.getCollectionVideoListFromBilibili(collectionId, pageSize, currentPn - 1,
                collectionVideoOrder).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    public void lazyInit() {
        if (this.videos == null) {
            // 第一次初始化
            // 先查总数
            log.info("初始化集合:");


            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                        factory.getCollectionVideoListFromBilibili(collectionId, pageSize, 1, collectionVideoOrder);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            } else {
                if (upVideosTotalNum == 0) {
                    IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                            factory.getCollectionVideoListFromBilibili(collectionId, pageSize, 1, collectionVideoOrder);
                    log.info("先获取一遍总数: {}", collectionVideoListFromBilibili.getTotalNum());
                    upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
                }
                IteratorCollectionTotalList<SimpleVideoInfo> collectionVideoListFromBilibili =
                        factory.getCollectionVideoListFromBilibili(collectionId, pageSize,
                                videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ? 1 : (upVideosTotalNum / pageSize) + 1,
                                collectionVideoOrder);
                videos = collectionVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = collectionVideoListFromBilibili.getTotalNum();
            }
        }
    }
}
