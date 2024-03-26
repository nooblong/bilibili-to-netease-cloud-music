package github.nooblong.download.netmusic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.netmusic.module.base.BaseModule;
import github.nooblong.download.netmusic.module.base.ModuleFactory;
import github.nooblong.download.netmusic.module.weapi.Login;
import github.nooblong.download.netmusic.module.weapi.LoginCellphone;
import github.nooblong.download.netmusic.module.weapi.LoginQrCheck;
import github.nooblong.download.netmusic.module.weapi.LoginRefresh;
import github.nooblong.download.utils.OkUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NetMusicClient {

    private final OkHttpClient templateClient;
    private final ModuleFactory moduleFactory;
    private final IUserService userService;

    public NetMusicClient(ModuleFactory moduleFactory, IUserService userService,
                          LoginQrCheck loginQrCheck,
                          LoginRefresh loginRefresh,
                          Login login,
                          LoginCellphone loginCellphone) {
        this.userService = userService;
        this.templateClient = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();
        this.moduleFactory = moduleFactory;
        this.loginQrCheck = loginQrCheck;
        this.loginRefresh = loginRefresh;
        this.login = login;
        this.loginCellphone = loginCellphone;
    }

    public OkHttpClient generateClient(Long userId, List<Cookie> cookiesByUser) {
        return templateClient.newBuilder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(@Nullable HttpUrl url, @Nullable List<Cookie> cookies) {
                        if (cookieLoginApi(url)) {
                            if (cookies != null && cookies.size() > 5) {
                                userService.updateNeteaseCookieByOkhttpCookie(userId, cookies);
                            }
                        }
//                        if (cookieRefreshApi(url)) {
//                            ObjectNode objectNode = CookieUtil.parseCookiesIn(cookies);
//                            try {
//                                String userNetCookiesStr = user.getNetCookies();
//                                ObjectNode userNetCookies = (ObjectNode) objectMapper.readTree(userNetCookiesStr);
//                                objectNode.fields().forEachRemaining(entry -> {
//                                    if (userNetCookies.has(entry.getKey()) && StrUtil.isNotBlank(entry.getValue().asText())) {
//                                        log.info("更新网易cookie: {}, 与之前相同? {}", entry.getKey(),
//                                                entry.getValue().asText().equals(userNetCookies.get(entry.getKey()).asText()));
//                                        userNetCookies.put(entry.getKey(), entry.getValue().asText());
//                                    }
//                                });
//                                user.setNetCookies(userNetCookies.toString());
//                            } catch (JsonProcessingException e) {
//                                log.error("刷新网易token出错: {}", e.getMessage());
//                            }
//                        }
                        if (url != null && url.toString().contains("/register/anonimous")) {
                            ObjectNode anonymousCookie = userService.cookieListToObjectNode(cookies);
                            if (anonymousCookie.has("MUSIC_A")) {
                                log.info("设置游客token成功!");
                                OkUtil.anonymousToken = anonymousCookie.get("MUSIC_A").asText();
                            }
                        }
                    }

                    @Override
                    @Nonnull
                    public List<Cookie> loadForRequest(@Nullable HttpUrl url) {
                        log.info("对: {} 使用用户cookie: {}", url, userId);
                        log.info("cookie: {}", cookiesByUser);
                        return cookiesByUser;
                    }
                })
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
    }

    public JsonNode getMusicDataByUserId(Map<String, Object> queryMap, String key, Long userId) {
        List<Cookie> cookiesByUser = userService.getNeteaseOkhttpCookie(userId);
        BaseModule baseModule = moduleFactory.getService(key);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode paramNode = objectMapper.createObjectNode();
        baseModule.genParams(paramNode, queryMap);
        Map<String, String> headerMap = new HashMap<>();
        baseModule.genHeader(headerMap);
        baseModule.genCookie(cookiesByUser);
        String csrfToken = getCsrfToken(cookiesByUser);
        if (csrfToken != null) {
            queryMap.put("csrfToken", csrfToken);
        }
        OkHttpClient client = generateClient(userId, cookiesByUser);
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

    public JsonNode getVoiceListDetail(String voiceListId, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("voiceListId", voiceListId);
        // 获取歌单信息
        return this.getMusicDataByUserId(params, "voiceListDetail", userId).get("data");
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
