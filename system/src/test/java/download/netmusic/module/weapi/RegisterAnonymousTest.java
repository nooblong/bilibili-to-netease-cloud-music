package download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import download.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class RegisterAnonymousTest extends BaseTest {

    @Test
    public void test() {
        HashMap<String, Object> map = new HashMap<>();
        JsonNode registeranonymous = netMusicClient.getMusicDataByUserId(map, "registeranonymous", 52L);
        System.out.println(registeranonymous.toPrettyString());
    }

}