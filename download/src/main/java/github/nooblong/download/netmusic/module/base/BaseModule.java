package github.nooblong.download.netmusic.module.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

public interface BaseModule {

    void genParams(ObjectNode node, Map<String, Object> queryMap);

    void genCookie(List<Cookie> cookieList);

    void genHeader(Map<String, String> headerMap);

    String getUrl();

    String getType();

    String getOptionsUrl();

    String getMethod();

    void postProcess(JsonNode response);

    JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client);
}
