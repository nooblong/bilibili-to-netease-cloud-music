package github.nooblong.download.netmusic.module.weapi;

import cn.hutool.core.util.XmlUtil;
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
import org.w3c.dom.Document;

import java.util.Map;

@Slf4j
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AudioUploadFirst extends SimpleWeApiModule {

    private String token;
    private String objectKey;

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        this.token = (String) queryMap.get("token");
        this.objectKey = ((String) queryMap.get("objectKey")).replace("/", "%2F");
        node.put("token", (String) queryMap.get("token"));
        node.put("objectKey", ((String) queryMap.get("objectKey")));
        node.put("bucket", (String) queryMap.get("bucket"));
        node.put("docId", (String) queryMap.get("docId"));
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("x-nos-token", token);
        headerMap.put("Host", "ymusic.nos-hz.163yun.com");
        headerMap.put("Content-Type", "audio/mpeg");
        headerMap.put("User-Agent", OkUtil.WEAPI_AGENT);
        headerMap.put("Connection", "close");
        headerMap.put("X-Nos-Meta-Content-Type", "audio/mpeg");
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        Document xmlResponse = OkUtil.getXmlResponse(OkUtil.postWeApi(objectMapper.createObjectNode(),
                getUrl(), headerMap, getMethod()), client);
        //nos.netease.com/doc/NOS_User_Manual.pdf
        assert xmlResponse != null;
        String uploadId = XmlUtil.getRootElement(xmlResponse).getElementsByTagName("UploadId").item(0).getTextContent();
        if (uploadId == null) {
            throw new RuntimeException("post upload错误");
        }
        return objectMapper.createObjectNode().put("uploadId", uploadId);
    }

    @Override
    public String getUrl() {
        return "https://ymusic.nos-hz.163yun.com/" + objectKey + "?uploads";
    }
}
