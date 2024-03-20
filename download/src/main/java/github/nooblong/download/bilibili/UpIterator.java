package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;


@Slf4j
public class UpIterator extends SimplePageIterator {
    private final String keyWord;
    private final UserVideoOrder userVideoOrder;
    String upId;


    public UpIterator(BilibiliBatchIteratorFactory factory, String upId, String keyWord, int limitSec,
                      VideoOrder videoOrder, UserVideoOrder userVideoOrder) {
        super(factory, limitSec, videoOrder);
        this.upId = upId;
        this.keyWord = keyWord;
        this.userVideoOrder = userVideoOrder;
        Assert.isTrue(videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ||
                videoOrder == VideoOrder.PUB_OLD_FIRST_THEN_NEW, "Up主迭代器不支持该排序类型");
    }

    @Override
    SimpleVideoInfo[] getNextPage(int currentPn, int pageSize) {
        return factory.getUpVideoListFromBilibili(upId, pageSize, currentPn + 1,
                userVideoOrder, keyWord).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize) {
        return factory.getUpVideoListFromBilibili(upId, pageSize, currentPn - 1,
                userVideoOrder, keyWord).getData().toArray(new SimpleVideoInfo[0]);
    }

    public void lazyInit() {
        if (this.videos == null) {
            log.info("初始化集合:");
            // 第一次初始化
            // 先查总数
            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                IteratorCollectionTotalList<SimpleVideoInfo> upVideoListFromBilibili = factory.getUpVideoListFromBilibili(upId, pageSize,
                        1,
                        userVideoOrder, keyWord);
                videos = upVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = upVideoListFromBilibili.getTotalNum();
            } else {
                if (upVideosTotalNum == 0) {
                    IteratorCollectionTotalList<SimpleVideoInfo> toGetCount = factory.getUpVideoListFromBilibili(upId, pageSize,
                            1,
                            userVideoOrder, keyWord);
                    log.info("先获取一遍总数: {}", toGetCount.getTotalNum());
                    upVideosTotalNum = toGetCount.getTotalNum();
                }
                IteratorCollectionTotalList<SimpleVideoInfo> upVideoListFromBilibili = factory.getUpVideoListFromBilibili(upId, pageSize,
                        videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ? 1 : (upVideosTotalNum / pageSize) + 1,
                        userVideoOrder, keyWord);
                videos = upVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = upVideoListFromBilibili.getTotalNum();
            }
        }
    }
}
