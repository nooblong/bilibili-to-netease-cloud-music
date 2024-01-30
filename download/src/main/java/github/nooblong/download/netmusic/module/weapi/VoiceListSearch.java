package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VoiceListSearch extends SimpleWeApiModule {
    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("fee", -1);
        node.put("limit", queryMap.get("limit") != null ? Integer.parseInt((String) queryMap.get("limit")) : 200);
        node.put("offset", queryMap.get("offset") != null ? Integer.parseInt((String) queryMap.get("offset")) : 0);
        node.put("podcastName", queryMap.get("limit") != null ? (String) queryMap.get("limit") : "");
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voicelist/search";
    }
}
