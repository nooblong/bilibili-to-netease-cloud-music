package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class LoginCellphoneTest extends BaseTest {
    @Test
    public void test() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("phone", "13814921933");
        hashMap.put("password", "*****");
        JsonNode logincellphone = netMusicClient.getMusicDataByUserId(hashMap, "logincellphone", 52L);
        System.out.println(logincellphone.toPrettyString());
    }
}