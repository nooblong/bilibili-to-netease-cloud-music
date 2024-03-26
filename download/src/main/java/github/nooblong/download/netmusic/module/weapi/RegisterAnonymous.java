package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RegisterAnonymous extends SimpleWeApiModule {

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.postWeApi(paramNode,
                getUrl()
                , headerMap, getMethod()), client);
        postProcess(jsonResponse);
        return jsonResponse;
    }

    /**
     * {
     * "code" : 200,
     * "userId" : 9330593063,
     * "createTime" : 1710045805070
     * }
     */


    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("username", "Tk1VU0lDIGdtVG82R2lvNEZoRWY5MFZqZzhPenc9PQ==");
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/register/anonimous";
    }
}
