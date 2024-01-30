package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Scope("prototype")
public class AudioPreCheck extends SimpleWeApiModule {

    String token;

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        token = (String) queryMap.get("token");

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("name", queryMap.get("name") != null ? (String) queryMap.get("name") : "空白的名称");
        objectNode.put("autoPublish", false);
        objectNode.put("autoPublishText", "");
        objectNode.put("description", queryMap.get("description") != null ? (String) queryMap.get("description") :
                "upload by nooblong/bilibili-to-netease-cloud-music");
        objectNode.put("voiceListId", (String) queryMap.get("voiceListId"));
        objectNode.put("coverImgId", (String) queryMap.get("coverImgId"));
        objectNode.put("dfsId", (String) queryMap.get("docId"));
        objectNode.put("categoryId", (String) queryMap.get("categoryId"));
        objectNode.put("secondCategoryId", (String) queryMap.get("secondCategoryId"));
        objectNode.set("composedSongs", objectMapper.createArrayNode()
//                .add(objectMapper.createObjectNode().put("type", 3)
//                        .put("title", "声音简介")
//                        .set("detail", objectMapper.createArrayNode()
//                .add(objectMapper.createObjectNode().put("type", 1)
//                        .put("content", "123123123123213")
//                        .put("publishTime", 0)
//                        .put("orderNo", 1))))
        );
        objectNode.put("privacy", Boolean.valueOf((String) queryMap.get("privacy")));
        objectNode.put("publishTime", "0");
        objectNode.put("orderNo", "1");
        ArrayNode arrayNode = objectMapper.createArrayNode().add(objectNode);
        node.put("voiceData", arrayNode.toString());
        node.put("dupkey", UUID.randomUUID().toString().replaceAll("\"\"", "\""));

        // "[{"name":"zhunitiantiankuaile","autoPublish":false,"autoPublishText":"",
        // "description":"123123123123213","voiceListId":994666636,"coverImgId":"109951168896496224",
        // "dfsId":"509951163305218925","categoryId":3,"secondCategoryId":469050,"composedSongs":[],
        // "privacy":true,"modules":"[{\"type\":3,\"title\":\"声音简介\",
        // \"detail\":[{\"type\":1,\"content\":\"123123123123213\\n\"}]}]","publishTime":0,"orderNo":1}]"
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("Host", "interface.music.163.com");
        headerMap.put("X-Real-IP:", "::1");
        headerMap.put("X-Forwarded-For:", "::1");
        headerMap.put("x-nos-token", token);
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voice/batch/upload/preCheck";
    }
}
