package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VoiceRemove extends SimpleWeApiModule {

    @Override
    public void genCookie(List<Cookie> cookieList) {
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
//        node.put("csrf_token", "e76e7e09a2b8314443848b3b21a53e9b");
        node.put("voiceIds", "[" + queryMap.get("voiceIds") + "]");
//        ObjectMapper objectMapper = new ObjectMapper();
//        ArrayNode arrayNode = objectMapper.createArrayNode();
//        arrayNode.add((Long) queryMap.get("voiceIds"));
//        node.set("voiceIds", arrayNode);
        node.put("voiceListId", (Long) queryMap.get("voiceListId"));
        // csrf_token:"e76e7e09a2b8314443848b3b21a53e9b"
        // voiceIds:"[2531835161]"
        // voiceListId:996002302
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/content/voicelist/remove/voice";
    }
}
