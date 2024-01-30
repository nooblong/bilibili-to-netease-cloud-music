package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AudioUploadAlloc extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("bucket", "ymusic");
        node.put("ext", (String) queryMap.get("ext"));
        node.put("filename", (String) queryMap.get("fileName"));
        node.put("local", false);
        node.put("nos_product", 0);
        node.put("type", "other");
    }

    @Override
    public void postProcess(JsonNode response) {
        assert response != null;
        if (!(response.get("code").asInt() == 200)) {
            throw new RuntimeException("allocated错误");
        }
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/nos/token/alloc";
    }
}
