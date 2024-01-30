package github.nooblong.download.utils;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import org.springframework.cache.annotation.CacheConfig;

import java.util.ArrayList;
import java.util.List;

@CacheConfig(cacheNames = "cookies")
@Slf4j
public class CookieUtil {

    public static List<Cookie> getCookiesByUser(SysUser user) {
        if (user == null) {
            return new ArrayList<>();
        }
        ObjectNode userCookieNode = getUserCookieNode(user);
        return node2cookies(userCookieNode);
    }

    private static ObjectNode getUserCookieNode(SysUser user) {
        String netCookies = user.getNetCookies();
        if (netCookies != null && !netCookies.isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return ((ObjectNode) objectMapper.readTree(netCookies));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new ObjectMapper().createObjectNode();
    }

    public static List<Cookie> node2cookies(ObjectNode userCookieNode) {
        List<Cookie> cookies = new ArrayList<>();
        userCookieNode.fields().forEachRemaining(stringJsonNodeEntry -> {
            cookies.add(new Cookie.Builder().name(stringJsonNodeEntry.getKey())
                    .value(stringJsonNodeEntry.getValue().asText())
                    .domain("music.163.com")
                    .build());
        });
        return cookies;
    }

    public static ObjectNode parseCookiesIn(List<Cookie> cookies) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        if (cookies == null) {
            return objectNode;
        }
        for (Cookie cookie : cookies) {
            if (StrUtil.isNotBlank(cookie.value())) {
                objectNode.put(cookie.name(), cookie.value());
            }
        }
        return objectNode;
    }

}