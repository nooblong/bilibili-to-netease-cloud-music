package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Scope("prototype")
public class ImageUploadSecond extends SimpleWeApiModule {
    private String size;
    private String imgX;
    private String imgY;
    private String docId;


    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        this.size = queryMap.get("size") == null ? "600" : (String) queryMap.get("size");
        this.imgX = queryMap.get("imgX") == null ? "0" : (String) queryMap.get("imgX");
        this.imgY = queryMap.get("imgY") == null ? "0" : (String) queryMap.get("imgY");
        this.docId = (String) queryMap.get("docId");
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        JsonNode imgResponse = OkUtil.getJsonResponse(OkUtil.get(
                "https://music.163.com/upload/img/op?id=" + docId
                        + "&op=" + imgX + "y" + imgY + "y" + 300 + "y" + 300), client);
        assert imgResponse != null;
        if (!(imgResponse.get("code").asInt() == 200)) {
            throw new RuntimeException("裁剪错误");
        }
        return imgResponse;
    }

    @Override
    public String getMethod() {
        return "put";
    }

    @Override
    public String getUrl() {
        return "";
    }
}
