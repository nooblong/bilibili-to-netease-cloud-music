package github.nooblong.download.bilibili;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.BaseTest;
import github.nooblong.download.utils.Constant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BilibiliUtilTest extends BaseTest {

    @Autowired
    BilibiliUtil bilibiliUtil;

    @Test
    void validate() throws JsonProcessingException {
        SysUser byId = Db.getById(Constant.adminUserId, SysUser.class);
        JsonNode cookie = new ObjectMapper().readTree(byId.getBiliCookies());
        bilibiliUtil.validate(cookie);
    }
}