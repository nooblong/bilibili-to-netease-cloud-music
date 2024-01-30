package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 更新歌单标签
 */
@Service
public class PlaylistTagsUpdate extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://interface3.music.163.com/eapi/playlist/tags/update";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/playlist/tags/update";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("id"));
        node.put("tags", (String) queryMap.get("tags"));
    }
}
