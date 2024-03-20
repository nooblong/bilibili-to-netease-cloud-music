package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BilibiliVideoTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;

    @Test
    void getBestStreamUrl1() {
        BilibiliVideo bilibiliVideo = new BilibiliVideo().setBvid("BV1ju411T7so");
        bilibiliClient.init(bilibiliVideo, bilibiliClient.getCurrentCred());
        System.out.println(bilibiliClient.getBestStreamUrl(bilibiliVideo, bilibiliClient.getCurrentCred()).toPrettyString());
        System.out.println(bilibiliVideo.getHasMultiPart());
        System.out.println(bilibiliVideo.getHasSeries());
    }

    @Test
    void getSeriesMeta1() {
        BilibiliVideo bilibiliVideo = new BilibiliVideo().setBvid("BV1ju411T7so");
        bilibiliClient.init(bilibiliVideo, bilibiliClient.getCurrentCred());
        System.out.println(bilibiliClient.getSeriesMeta(bilibiliVideo.getMySeriesId(), bilibiliClient.getCurrentCred()).toPrettyString());
    }

    @Test
    void getUpVideos1() {
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos("8356881", 30, 1, UserVideoOrder.FAVORITE, "", bilibiliClient.getCurrentCred());
        System.out.println(collectionTotal.getData().toPrettyString());
    }
}