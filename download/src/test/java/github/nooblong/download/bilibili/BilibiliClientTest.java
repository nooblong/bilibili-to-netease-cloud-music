package github.nooblong.download.bilibili;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class BilibiliClientTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;


    @Test
    void getUpChannels() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        JsonNode upChannels = bilibiliClient.getUpChannels("631070414", availableBilibiliCookie);
        System.out.println(upChannels.toPrettyString());
    }

    @Test
    void getPart() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        bilibiliClient.getPartVideosFromBilibili("BV1vhADevEgB", availableBilibiliCookie);
    }

    @Test
    void testApi() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        System.out.println(availableBilibiliCookie);
        JsonNode userFavoriteList = bilibiliClient.getUserFavoriteList("6906052", availableBilibiliCookie);
        System.out.println(userFavoriteList.toPrettyString());
    }

    @Test
    void getBestStreamUrl1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        System.out.println(bilibiliClient.getBestStreamUrl(bilibiliFullVideo, availableBilibiliCookie).toPrettyString());
        System.out.println(bilibiliFullVideo.getHasMultiPart());
        System.out.println(bilibiliFullVideo.getHasSeries());
    }

    @Test
    void getSeriesMeta1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        System.out.println(bilibiliClient.getSeriesMeta(bilibiliFullVideo.getMySeriesId(), availableBilibiliCookie).toPrettyString());
    }

    @Test
    void getSeriesMeta2() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
//        SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid("BV1ju411T7so");
//        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        System.out.println(bilibiliClient.getOldSeriesMeta("1869296", availableBilibiliCookie).toPrettyString());
    }

    @Test
    void getUpVideos1() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        IteratorCollectionTotal collectionTotal = bilibiliClient.getUpVideos("631070414", 30, 1,
                UserVideoOrder.PUBDATE, "", availableBilibiliCookie);
        System.out.println(collectionTotal.getData().toPrettyString());
    }
}