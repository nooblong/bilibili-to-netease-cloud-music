package github.nooblong.btncm.netmusic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

/**
 * 每个网易云接口实现这个
 */
public interface BaseModule {

    /**
     * 请求需要附带的参数
     */
    void genParams(ObjectNode node, Map<String, Object> queryMap);

    /**
     * 请求需要附带的cookie
     */
    void genCookie(List<Cookie> cookieList);

    /**
     * 请求需要附带的header
     */
    void genHeader(Map<String, String> headerMap);

    /**
     * 请求地址
     */
    String getUrl();

    /**
     * 请求类型（weapi、eapi、、、）
     */
    String getType();

    /**
     * 请求方法
     */
    String getMethod();

    /**
     * 请求后处理，处理后返回NetMusicClient
     */
    void postProcess(JsonNode response);

    /**
     * 请求需要特殊处理，不使用SimpleWeApiModule的简单请求
     */
    JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client);
}
