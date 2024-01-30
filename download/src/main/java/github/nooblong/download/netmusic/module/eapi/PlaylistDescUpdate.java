package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 更新歌单描述
 */
@Service
public class PlaylistDescUpdate extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://interface3.music.163.com/eapi/playlist/desc/update";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/playlist/desc/update";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("id"));
        node.put("desc", (String) queryMap.get("desc"));
    }
}
