package github.nooblong.download.netmusic;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class NetMusicClientTest extends BaseTest {

    @Test
    void checkLogin() {
        boolean b = netMusicClient.checkLogin(1L);
        System.out.println(b);
    }

    @Test
    public void testTrans() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("programId", "2532916453");
        hashMap.put("radioId", "996517258");
        hashMap.put("position", "999");
//        hashMap.put("useCookie", "1");
        JsonNode audioprogramtrans = netMusicClient.getMusicDataByUserId(hashMap, "audioprogramtrans", 1L);
        System.out.println(audioprogramtrans.toPrettyString());
    }
}