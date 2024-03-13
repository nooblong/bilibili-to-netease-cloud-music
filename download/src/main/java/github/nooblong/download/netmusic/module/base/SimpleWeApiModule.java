package github.nooblong.download.netmusic.module.base;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class SimpleWeApiModule implements BaseModule {

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        JsonNode jsonResponse = OkUtil.getJsonResponse(OkUtil.postWeApi(paramNode,
                getUrl()
                , headerMap, getMethod()), client);
        postProcess(jsonResponse);
        return jsonResponse;
    }

    @Override
    public String getType() {
        return "weapi";
    }

    @Override
    public String getOptionsUrl() {
        return null;
    }

    @Override
    public abstract void genParams(ObjectNode node, Map<String, Object> queryMap);

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("__remember_me").value("true").build());
        Random random = new Random();
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        cookieList.add(OkUtil.netCookieBuilder().name("_ntes_nuid").value(sb.toString()).build());
        if (!getUrl().contains("login")) {
            random.nextBytes(randomBytes);
            StringBuilder sb2 = new StringBuilder();
            for (byte b : randomBytes) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            cookieList.add(OkUtil.netCookieBuilder().name("NMTID").value(sb2.toString()).build());
        }
        boolean hasMusicU = false;
        boolean hasMusicA = false;
        for (Cookie cookie : cookieList) {
            if (cookie.name().equals("MUSIC_U") && StrUtil.isNotBlank(cookie.value())) {
                hasMusicU = true;
            }
            if (cookie.name().equals("MUSIC_A") && StrUtil.isNotBlank(cookie.value())) {
                hasMusicA = true;
            }
        }
        if (!hasMusicU && !hasMusicA) {
            cookieList.add(OkUtil.netCookieBuilder().name("MUSIC_A").value(OkUtil.anonymousToken).build());
            cookieList.add(OkUtil.netCookieBuilder().name("os").value("iOS").build());
            cookieList.add(OkUtil.netCookieBuilder().name("appver").value("8.20.21").build());
        }
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
