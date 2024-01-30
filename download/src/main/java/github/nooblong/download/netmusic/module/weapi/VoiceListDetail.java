package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VoiceListDetail extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("voiceListId"));
        node.put("csrf_token", (String) queryMap.get("csrf_token"));
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voicelist/detail";
    }
}
