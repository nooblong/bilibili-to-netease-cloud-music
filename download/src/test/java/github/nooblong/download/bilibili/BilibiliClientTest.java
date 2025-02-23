package github.nooblong.download.bilibili;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.BaseTest;
import github.nooblong.download.entity.IteratorCollectionTotalList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class BilibiliClientTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;


    @Test
    void getUpChannels() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        JsonNode upChannels = bilibiliClient.getUpChannels("631070414", availableBilibiliCookie);
        System.out.println(upChannels.toPrettyString());
    }

    @Test
    void getPart() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        bilibiliClient.getPartVideosFromBilibili("BV1vhADevEgB", availableBilibiliCookie);
    }

    @Test
    void testApi() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        System.out.println(availableBilibiliCookie);
        JsonNode userFavoriteList = bilibiliClient.getUserFavoriteList("6906052", availableBilibiliCookie);
        System.out.println(userFavoriteList.toPrettyString());
    }
}