package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImageUploadFirst extends SimpleWeApiModule {

    private String token;
    private String objectKey;
    private InputStream inputStream;

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        this.token = (String) queryMap.get("token");
        this.objectKey = ((String) queryMap.get("objectKey")).replace("/", "%2F");
        node.put("token", (String) queryMap.get("token"));
        node.put("objectKey", ((String) queryMap.get("objectKey")));
        node.put("bucket", (String) queryMap.get("bucket"));
        node.put("docId", (String) queryMap.get("docId"));
        // param
        String path = (String) queryMap.get("imagePath");
        try {
            inputStream = new FileInputStream(Paths.get(path).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("x-nos-token", token);
        headerMap.put("Host", "ymusic.nos-hz.163yun.com");
        headerMap.put("Content-Type", "image/jpeg");
        headerMap.put("User-Agent", OkUtil.getFixedUserAgent(0));
        headerMap.put("Connection", "close");
        headerMap.put("X-Nos-Meta-Content-Type", "image/jpeg");
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.uploadImgWeapi(inputStream, token, objectKey), client);
        assert jsonResponse != null;
        return objectMapper.createObjectNode();
    }

    @Override
    public String getUrl() {
        return "https://ymusic.nos-hz.163yun.com/" + objectKey + "?uploads";
    }
}
