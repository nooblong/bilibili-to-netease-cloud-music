package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VoiceList extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("offset", ((Long) queryMap.get("offset")));
        node.put("limit", 20);
        node.put("voiceListId", (String) queryMap.get("voiceListId"));
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voices/by/voicelist";
    }
}
