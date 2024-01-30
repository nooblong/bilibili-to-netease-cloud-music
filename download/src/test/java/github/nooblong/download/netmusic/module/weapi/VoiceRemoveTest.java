package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


class VoiceRemoveTest extends BaseTest {

    @Test
    public void test() {
        Map<String, Object> map = new HashMap<>();
        map.put("voiceIds", 2534918666L);
        map.put("voiceListId", 994819294L);
        JsonNode voiceRemove = netMusicClient.getMusicDataByUserId(map, "voiceremove", 1L);
        System.out.println(voiceRemove.toPrettyString());
    }

}