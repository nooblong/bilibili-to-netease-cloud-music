package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class SimpleVideoInfoTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;

    @Test
    void getBestStreamUrl1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        System.out.println(bilibiliClient.getBestStreamUrl(bilibiliFullVideo, availableBilibiliCookie).toPrettyString());
        System.out.println(bilibiliFullVideo.getHasMultiPart());
        System.out.println(bilibiliFullVideo.getHasSeries());
    }

    @Test
    void getSeriesMeta1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        System.out.println(bilibiliClient.getSeriesMeta(bilibiliFullVideo.getMySeriesId(), availableBilibiliCookie).toPrettyString());
    }

    @Test
    void getUpVideos1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos("8356881", 30, 1,
                UserVideoOrder.FAVORITE, "", availableBilibiliCookie);
        System.out.println(collectionTotal.getData().toPrettyString());
    }
}