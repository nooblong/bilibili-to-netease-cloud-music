package github.nooblong.download.netmusic.module.api;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author wjning
 * @date 2021/6/7 16:43
 * @description 歌单详情
 */
@Service
public class PlaylistDetail extends SimpleApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("id"));
        node.put("n", 100000);
        node.put("s", StrUtil.nullToDefault((String) queryMap.get("s"), "8"));
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/api/v6/playlist/detail";
    }
}
