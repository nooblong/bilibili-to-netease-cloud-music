package github.nooblong.btncm.netmusic.api.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.btncm.netmusic.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LoginQrKey extends SimpleWeApiModule {

    @Override
    public void postProcess(JsonNode response) {
        if (response.isObject()) {
            ObjectNode resp = (ObjectNode) response;
            String text = resp.get("unikey").asText().replaceAll("\"", "");
            resp.put("unikey", text);
        }
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("type", 3);
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/login/qrcode/unikey";
    }
}
