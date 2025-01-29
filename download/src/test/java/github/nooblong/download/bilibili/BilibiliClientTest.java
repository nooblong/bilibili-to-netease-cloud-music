package github.nooblong.download.bilibili;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class BilibiliClientTest extends BaseTest {

    @Autowired
    BilibiliClient bilibiliClient;

    @Test
    void validate() throws JsonProcessingException {
        SysUser byId = Db.getById(1L, SysUser.class);
        Map<String, String> bilibiliCookieMap = userService.getBilibiliCookieMap(1L);
        bilibiliClient.validate(bilibiliCookieMap, 1L);
    }

    @Test
    void getUpChannels() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        JsonNode upChannels = bilibiliClient.getUpChannels("631070414", availableBilibiliCookie);
        System.out.println(upChannels.toPrettyString());
    }

    @Test
    void testApi() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        System.out.println(availableBilibiliCookie);
        JsonNode userFavoriteList = bilibiliClient.getUserFavoriteList("6906052", availableBilibiliCookie);
        System.out.println(userFavoriteList.toPrettyString());
    }
}