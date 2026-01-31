package github.nooblong.download.netmusic.module.get;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleGetTypeModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 二维码
 */
@Service
public class LoginQrCreate extends SimpleGetTypeModule {

    private String key;

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("key", (String) queryMap.get("key"));
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/login?codekey=" + this.key;
    }
}
