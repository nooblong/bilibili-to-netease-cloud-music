package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

class RegisterAnonymousTest extends BaseTest {

    @Test
    public void test() {
        HashMap<String, Object> map = new HashMap<>();
        JsonNode registeranonymous = netMusicClient.getMusicDataByUserId(map, "registeranonymous", 52L);
        System.out.println(registeranonymous.toPrettyString());
    }

    @Test
    public void testConvert() {
        String input = "a";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        System.out.println(Arrays.toString(bytes));
    }

}