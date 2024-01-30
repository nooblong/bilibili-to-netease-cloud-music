package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AudioUploadThird extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("token", (String) queryMap.get("token"));
        node.set("uploadResult", (ArrayNode) queryMap.get("uploadResult"));
        node.put("objectKey", ((String) queryMap.get("objectKey")).replace("/", "%2F"));
        node.put("uploadId", (String) queryMap.get("uploadId"));
        Assert.notNull(queryMap.get("token"), "AudioUploadThird缺少参数");
        Assert.notNull(queryMap.get("uploadResult"), "AudioUploadThird缺少参数");
        Assert.notNull(queryMap.get("objectKey"), "AudioUploadThird缺少参数");
        Assert.notNull(queryMap.get("uploadId"), "AudioUploadThird缺少参数");
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ArrayNode uploadResult = ((ArrayNode) paramNode.get("uploadResult"));
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (JsonNode jsonNode : uploadResult) {
            sb.append("<Part>")
                    .append("<PartNumber>")
                    .append(i++)
                    .append("</PartNumber>")
                    .append("<ETag>")
                    .append(jsonNode.get("etag").asText())
                    .append("</ETag>")
                    .append("</Part>");
        }

        Map<String, String> xmlHeader = new HashMap<>();
        xmlHeader.put("X-Nos-Meta-Content-Type", "audio/mpeg");
        xmlHeader.put("x-nos-token", paramNode.get("token").asText());
        xmlHeader.put("Content-Type", "text/plain;charset=UTF-8");
        String body = "<CompleteMultipartUpload>\n  " + sb + "   \n     </CompleteMultipartUpload>";
        String xmlResponse = OkUtil.getStringResponse(OkUtil.post(body, "text/plain; charset=utf-8",
                "https://ymusic.nos-hz.163yun.com/" + paramNode.get("objectKey").asText()
                        + "?uploadId=" + paramNode.get("uploadId").asText(), xmlHeader, "post"
        ), client);
        assert xmlResponse != null;
        return new ObjectMapper().createObjectNode();
    }

    @Override
    public String getUrl() {
        return "";
    }
}
