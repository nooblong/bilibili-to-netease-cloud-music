package github.nooblong.download.netmusic.module.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

public abstract class SimpleEApiModule implements BaseModule {

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.postEApi(paramNode, getUrl(), getOptionsUrl(),
                headerMap, getMethod()), client);
        postProcess(jsonResponse);
        return jsonResponse;
    }

    @Override
    public String getType() {
        return "eapi";
    }

    @Override
    public String getOptionsUrl() {
        return null;
    }

    @Override
    public abstract void genParams(ObjectNode node, Map<String, Object> queryMap);

    @Override
    public void genCookie(List<Cookie> cookieList) {

    }

    @Override
    public void genHeader(Map<String, String> headerMap) {

    }

    @Override
    public abstract String getUrl();

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public void postProcess(JsonNode response) {

    }
}
