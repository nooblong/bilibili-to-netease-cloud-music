package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 初始化昵称
 */
@Service
public class ActivateInitProfile extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://music.163.com/eapi/activate/initProfile";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/activate/initProfile";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("nickname", (String) queryMap.get("nickname"));
    }
}
