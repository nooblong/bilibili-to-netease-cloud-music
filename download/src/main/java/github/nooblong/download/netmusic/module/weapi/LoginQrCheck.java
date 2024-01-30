package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class LoginQrCheck extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("key", (String) queryMap.get("key"));
        node.put("type", 1);
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/login/qrcode/client/login";
    }

}
