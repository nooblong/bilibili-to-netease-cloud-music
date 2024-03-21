package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SimpleVideoInfoTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;

    @Test
    void getBestStreamUrl1() {
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
        System.out.println(bilibiliClient.getBestStreamUrl(bilibiliFullVideo, bilibiliClient.getCurrentCred()).toPrettyString());
        System.out.println(bilibiliFullVideo.getHasMultiPart());
        System.out.println(bilibiliFullVideo.getHasSeries());
    }

    @Test
    void getSeriesMeta1() {
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
        System.out.println(bilibiliClient.getSeriesMeta(bilibiliFullVideo.getMySeriesId(), bilibiliClient.getCurrentCred()).toPrettyString());
    }

    @Test
    void getUpVideos1() {
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos("8356881", 30, 1, UserVideoOrder.FAVORITE, "", bilibiliClient.getCurrentCred());
        System.out.println(collectionTotal.getData().toPrettyString());
    }
}