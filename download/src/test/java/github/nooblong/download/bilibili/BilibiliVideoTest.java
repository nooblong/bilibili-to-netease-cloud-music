package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.IteratorCollectionTotal;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BilibiliVideoTest extends BaseTest {

    @Autowired
    BilibiliUtil bilibiliUtil;

    @Test
    void getBestStreamUrl1() {
        BilibiliVideo bilibiliVideo = new BilibiliVideo().setBvid("BV1ju411T7so");
        bilibiliUtil.init(bilibiliVideo, bilibiliUtil.getCurrentCred());
        System.out.println(bilibiliUtil.getBestStreamUrl(bilibiliVideo, bilibiliUtil.getCurrentCred()).toPrettyString());
        System.out.println(bilibiliVideo.getHasMultiPart());
        System.out.println(bilibiliVideo.getHasSeries());
    }

    @Test
    void getSeriesMeta1() {
        BilibiliVideo bilibiliVideo = new BilibiliVideo().setBvid("BV1ju411T7so");
        bilibiliUtil.init(bilibiliVideo, bilibiliUtil.getCurrentCred());
        System.out.println(bilibiliUtil.getSeriesMeta(bilibiliVideo.getMySeriesId(), bilibiliUtil.getCurrentCred()).toPrettyString());
    }

    @Test
    void getUpVideos1() {
        IteratorCollectionTotal collectionTotal = bilibiliUtil.getUpVideos("8356881", 30, 1, UserVideoOrder.FAVORITE, "", bilibiliUtil.getCurrentCred());
        System.out.println(collectionTotal.getData().toPrettyString());
    }
}