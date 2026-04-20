package github.nooblong.btncm.netmusic;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.utils.CommonUtil;
import github.nooblong.btncm.netmusic.api.weapi.Login;
import github.nooblong.btncm.netmusic.api.weapi.LoginCellphone;
import github.nooblong.btncm.netmusic.api.weapi.LoginQrCheck;
import github.nooblong.btncm.netmusic.api.weapi.LoginRefresh;
import github.nooblong.btncm.utils.OkUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NetMusicClient {

    private final OkHttpClient templateClient;
    private final ApplicationContext applicationContext;
    private final IUserService userService;
    final LoginQrCheck loginQrCheck;
    final LoginRefresh loginRefresh;
    final Login login;
    final LoginCellphone loginCellphone;

    public NetMusicClient(ApplicationContext applicationContext, IUserService userService,
                          LoginQrCheck loginQrCheck,
                          LoginRefresh loginRefresh,
                          Login login,
                          LoginCellphone loginCellphone) {
        this.userService = userService;
        this.templateClient = new OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .build();
        this.applicationContext = applicationContext;
        this.loginQrCheck = loginQrCheck;
        this.loginRefresh = loginRefresh;
        this.login = login;
        this.loginCellphone = loginCellphone;
    }

    public OkHttpClient generateClient(Long userId, List<Cookie> cookiesByUser) {
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        // 安装信任管理器
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        // 创建一个允许所有主机名的 HostnameVerifier
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return templateClient.newBuilder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(@Nullable HttpUrl url, @Nullable List<Cookie> cookies) {
                        if (url != null && (
                                url.toString().equals(loginQrCheck.getUrl()) ||
                                        url.toString().equals(login.getUrl()) ||
                                        url.toString().equals(loginCellphone.getUrl())
                        )) {
                            if (cookies != null && cookies.size() > 5) {
                                userService.updateNeteaseCookieByOkhttpCookie(userId, cookies);
                            }
                        }
                        if (url != null && (url.toString().equals(loginRefresh.getUrl()))) {
                            log.info("遇到网易用户set-cookie请求, 用户id: {}", userId);
                            try {
                                ObjectNode objectNode = CommonUtil.cookieListToObjectNode(cookies);
                                Map<String, String> neteaseCookieMap = userService.getNeteaseCookieMap(userId);
                                objectNode.fields().forEachRemaining(entry -> {
                                    if (neteaseCookieMap.containsKey(entry.getKey()) && StrUtil.isNotBlank(entry.getValue().asText())) {
                                        log.info("更新cookie中的: {}", entry.getKey());
                                        neteaseCookieMap.put(entry.getKey(), entry.getValue().asText());
                                    }
                                });
                                userService.updateNeteaseCookieByCookieMap(userId, neteaseCookieMap);
                                log.info("刷新网易cookie成功, 用户id: {}", userId);
                            } catch (Exception e) {
                                log.error("刷新网易cookie出错: {}", e.getMessage());
                            }
                        }
                        if (url != null && url.toString().contains("/register/anonimous")) {
                            log.info("? 遇到网易游客set-token请求");
                            ObjectNode anonymousCookie = CommonUtil.cookieListToObjectNode(cookies);
                            if (anonymousCookie.has("MUSIC_A")) {
                                log.info("设置游客token成功!");
                                OkUtil.anonymousToken = anonymousCookie.get("MUSIC_A").asText();
                            }
                        }
                    }

                    @Override
                    @Nonnull
                    public List<Cookie> loadForRequest(@Nullable HttpUrl url) {
                        return cookiesByUser;
                    }
                })
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
    }

    public JsonNode getMusicDataByUserId(Map<String, Object> queryMap, String key, Long userId) {
        List<Cookie> cookiesByUser = userService.getNeteaseOkhttpCookie(userId);
        BaseModule baseModule = applicationContext.getBean(key, BaseModule.class);
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

    public boolean checkLogin(Long userId) {
        JsonNode loginstatus = getMusicDataByUserId(new HashMap<>(), "LoginStatus", userId);
        return loginstatus.has("account") &&
                loginstatus.get("account").get("id") != null &&
                loginstatus.get("profile") != null &&
                loginstatus.get("profile").get("userId") != null;
    }

    // -------------------------------Common Api--------------------------------------

    public JsonNode getUserVoiceList(Long userId) {
        Map<String, Object> queryMap = new HashMap<>();
        return this.getMusicDataByUserId(queryMap, "voiceListSearch", userId).get("data");
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

}
