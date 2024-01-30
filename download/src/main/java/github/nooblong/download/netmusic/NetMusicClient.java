package github.nooblong.download.netmusic;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.netmusic.module.base.BaseModule;
import github.nooblong.download.netmusic.module.base.ModuleFactory;
import github.nooblong.download.netmusic.module.weapi.Login;
import github.nooblong.download.netmusic.module.weapi.LoginCellphone;
import github.nooblong.download.netmusic.module.weapi.LoginQrCheck;
import github.nooblong.download.netmusic.module.weapi.LoginRefresh;
import github.nooblong.download.utils.CookieUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class NetMusicClient {

    private final OkHttpClient noCookieClient;
    private final OkHttpClient templateClient;
    private final ModuleFactory moduleFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NetMusicClient(OkHttpClient client,
                          ModuleFactory moduleFactory,
                          LoginQrCheck loginQrCheck,
                          LoginRefresh loginRefresh,
                          Login login,
                          LoginCellphone loginCellphone) {
        this.templateClient = client;
        this.moduleFactory = moduleFactory;
        this.noCookieClient = client.newBuilder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        this.loginQrCheck = loginQrCheck;
        this.loginRefresh = loginRefresh;
        this.login = login;
        this.loginCellphone = loginCellphone;
    }

    public JsonNode getMusicDataByContext(Map<String, Object> params, String key) {
        SysUser user = JwtUtil.verifierFromContext();
        List<Cookie> cookiesByUser = CookieUtil.getCookiesByUser(user);
        OkHttpClient client = generateClient(user, cookiesByUser);
        return getMusicData(params, key, client, getCsrfToken(cookiesByUser), cookiesByUser);
    }

    public JsonNode getMusicDataByUserId(Map<String, Object> queryMap, String key, Long userId) {
        SysUser user = Db.getById(userId, SysUser.class);
        List<Cookie> cookiesByUser = CookieUtil.getCookiesByUser(user);
        OkHttpClient client = generateClient(user, cookiesByUser);
        return getMusicData(queryMap, key, client, getCsrfToken(cookiesByUser), cookiesByUser);
    }

    OkHttpClient generateClient(SysUser user, List<Cookie> cookiesByUser) {
        return templateClient.newBuilder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(@Nullable HttpUrl url, @Nullable List<Cookie> cookies) {
                        if (cookieLoginApi(url)) {
                            if (cookies != null && cookies.size() > 5) {
                                ObjectNode objectNode = CookieUtil.parseCookiesIn(cookies);
                                user.setNetCookies(objectNode.toString());
                                Db.updateById(user);
                            }
                        }
                        if (cookieRefreshApi(url)) {
                            ObjectNode objectNode = CookieUtil.parseCookiesIn(cookies);
                            try {
                                String userNetCookiesStr = user.getNetCookies();
                                ObjectNode userNetCookies = (ObjectNode) objectMapper.readTree(userNetCookiesStr);
                                objectNode.fields().forEachRemaining(entry -> {
                                    if (userNetCookies.has(entry.getKey()) && StrUtil.isNotBlank(entry.getValue().asText())) {
                                        log.info("更新cookie: {}, 与之前相同? {}", entry.getKey(),
                                                entry.getValue().asText().equals(userNetCookies.get(entry.getKey()).asText()));
                                        userNetCookies.put(entry.getKey(), entry.getValue().asText());
                                    }
                                });
                                user.setNetCookies(userNetCookies.toString());
                            } catch (JsonProcessingException e) {
                                log.error("刷新token出错: {}", e.getMessage());
                            }
                        }
                    }

                    @Override
                    @Nonnull
                    public List<Cookie> loadForRequest(@Nullable HttpUrl url) {
                        log.info("对: {} 使用用户cookie: {}", url, user.getUsername());
                        return cookiesByUser;
                    }
                })
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
    }

    public JsonNode getMusicDataWithNoCookie(Map<String, Object> queryMap, String key) {
        return getMusicData(queryMap, key, this.noCookieClient, null, new ArrayList<>());
    }

    public JsonNode getMusicData(Map<String, Object> queryMap, String key, OkHttpClient client,
                                 String csrfToken, List<Cookie> cookieList) {
        BaseModule baseModule = moduleFactory.getService(key);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode paramNode = objectMapper.createObjectNode();
        baseModule.genParams(paramNode, queryMap);
        Map<String, String> headerMap = new HashMap<>();
        baseModule.genHeader(headerMap);
        baseModule.genCookie(cookieList);
        if (csrfToken != null) {
            queryMap.put("csrfToken", csrfToken);
        }
        JsonNode result = baseModule.execute(paramNode, headerMap, client);
        baseModule.postProcess(result);
        return result;
    }

    private String getCsrfToken(List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if (cookie.name().equals("__csrf")) {
                return cookie.value();
            }
        }
        return null;
    }

    final LoginQrCheck loginQrCheck;
    final LoginRefresh loginRefresh;
    final Login login;
    final LoginCellphone loginCellphone;

    private boolean cookieLoginApi(HttpUrl url) {
        return url != null && (
                url.toString().equals(loginQrCheck.getUrl()) ||
                        url.toString().equals(login.getUrl()) ||
                        url.toString().equals(loginCellphone.getUrl())
        )
                ;
    }

    private boolean cookieRefreshApi(HttpUrl url) {
        return url != null && (
                url.toString().equals(loginRefresh.getUrl())
        )
                ;
    }

    public JsonNode getVoiceListDetail(String voiceListId) {
        Map<String, Object> params = new HashMap<>();
        params.put("voiceListId", voiceListId);
        // 获取歌单信息
        return this.getMusicDataByContext(params, "voiceListDetail").get("data");
    }

    public JsonNode getVoiceList(String voiceListId, Long userId, Long offset) {
        Map<String, Object> params = new HashMap<>();
        params.put("offset", offset);
        params.put("voiceListId", voiceListId);
        // 获取歌单信息
        return this.getMusicDataByUserId(params, "voiceList", userId).get("data");
    }

    public JsonNode getVoiceListDetailByUserId(String voiceListId, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("voiceListId", voiceListId);
        // 获取歌单信息
        return this.getMusicDataByUserId(params, "voiceListDetail", userId).get("data");
    }

    public boolean checkLogin(Long userId) {
        JsonNode loginstatus = getMusicDataByUserId(new HashMap<>(), "loginstatus", userId);
        return loginstatus.has("account") &&
                loginstatus.get("account").get("id") != null;
    }

}
