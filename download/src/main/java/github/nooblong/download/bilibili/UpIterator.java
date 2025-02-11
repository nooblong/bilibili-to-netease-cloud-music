package github.nooblong.download.bilibili;

import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.VideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class UpIterator extends SimplePageIterator {
    private final String keyWord;
    private final UserVideoOrder userVideoOrder;
    String upId;
    // 从第几页的第1/最后开始遍历
    int lastTotalIndexUp = 1;


    public UpIterator(BilibiliClient bilibiliClient, String upId, String keyWord, int limitSec,
                      VideoOrder videoOrder, UserVideoOrder userVideoOrder, boolean checkPart,
                      Map<String, String> bilibiliCookie, Integer lastTotalIndex, String channelIds,
                      AtomicInteger counter) {
        super(bilibiliClient, limitSec, videoOrder, checkPart, bilibiliCookie, lastTotalIndex, channelIds, counter);
        this.upId = upId;
        this.keyWord = keyWord;
        this.userVideoOrder = userVideoOrder;
        if (lastTotalIndex > 0) {
            this.lastTotalIndexUp = lastTotalIndex;
        }
        Assert.isTrue(videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD ||
                videoOrder == VideoOrder.PUB_OLD_FIRST_THEN_NEW, "Up主迭代器不支持该排序类型");
    }

    @Override
    SimpleVideoInfo[] getNextPage(int currentPn, int pageSize) {
        return bilibiliClient.getUpVideoListFromBilibili(upId, pageSize, currentPn + 1,
                userVideoOrder, keyWord, bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    @Override
    SimpleVideoInfo[] getPreviousPage(int currentPn, int pageSize) {
        return bilibiliClient.getUpVideoListFromBilibili(upId, pageSize, currentPn - 1,
                userVideoOrder, keyWord, bilibiliCookie).getData().toArray(new SimpleVideoInfo[0]);
    }

    public void lazyInit() {
        if (this.videos == null) {
            log.info("up初始化集合:");
            // 第一次初始化
            // 先查总数
            if (videoOrder == VideoOrder.PUB_NEW_FIRST_THEN_OLD) {
                IteratorCollectionTotalList<SimpleVideoInfo> upVideoListFromBilibili = bilibiliClient.getUpVideoListFromBilibili(upId, pageSize,
                        lastTotalIndexUp,
                        userVideoOrder, keyWord, bilibiliCookie);
                videos = upVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = upVideoListFromBilibili.getTotalNum();
                this.totalIndex += (lastTotalIndexUp - 1) * pageSize;
                this.currentPageNo = lastTotalIndexUp;
                log.info("up获取总数和第一页: {}", upVideoListFromBilibili.getTotalNum());
            } else {
                if (upVideosTotalNum == 0) {
                    IteratorCollectionTotalList<SimpleVideoInfo> toGetCount = bilibiliClient.getUpVideoListFromBilibili(upId, pageSize,
                            1,
                            userVideoOrder, keyWord, bilibiliCookie);
                    log.info("up先获取一遍总数: {}", toGetCount.getTotalNum());
                    upVideosTotalNum = toGetCount.getTotalNum();
                }
                IteratorCollectionTotalList<SimpleVideoInfo> upVideoListFromBilibili = bilibiliClient.getUpVideoListFromBilibili(upId, pageSize,
                        (upVideosTotalNum / pageSize) + 1 - lastTotalIndexUp + 1,
                        userVideoOrder, keyWord, bilibiliCookie);
                if (lastTotalIndexUp <= 2) {
                    this.totalIndex += upVideosTotalNum % pageSize;
                } else {
                    this.totalIndex += (upVideosTotalNum % pageSize) + ((lastTotalIndexUp - 2) * pageSize);
                }
                this.currentPageNo = (upVideosTotalNum / pageSize) + 1 - lastTotalIndexUp + 1;
                videos = upVideoListFromBilibili.getData().toArray(new SimpleVideoInfo[0]);
                upVideosTotalNum = upVideoListFromBilibili.getTotalNum();
            }
        }
    }
}
