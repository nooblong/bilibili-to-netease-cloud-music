package github.nooblong.download.bilibili;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
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
}