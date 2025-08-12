package github.nooblong.common.util;

import ch.qos.logback.classic.Level;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CommonUtil {

    public static void debug() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.DEBUG);
        logger.debug("debug mode");
    }

    public static String getExceptionStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static String processString(String input) {
        if (StrUtil.isBlank(input)) {
            return "";
        }
        String[] lines = input.split("\\r?\\n");
        if (lines.length <= 20) {
            return input; // 不超过100行，返回全部字符串
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = lines.length - 19; i < lines.length; i++) {
                result.append(lines[i]).append("\n");
            }
            return result.toString();
        }
    }

    public static String limitString(String input) {
        if (input == null) {
            return "";
        }
        return input.length() > 30 ? input.substring(0, 30) : input;
    }

    public static String limitString(String input, Integer num) {
        if (input == null) {
            return "";
        }
        return input.length() > num ? input.substring(0, num) : input;
    }

    // 将字符串转换为 List<String>，去除空格和空项
    public static List<String> toList(String str) {
        if (str == null || str.isEmpty()) {
            return List.of(); // 返回空列表
        }
        return Arrays.stream(str.split(","))
                .map(String::trim) // 去除多余空格
                .filter(s -> !s.isEmpty()) // 过滤空字符串
                .collect(Collectors.toList());
    }

    // 将 List<String> 转换为逗号分隔的字符串
    public static String toCommaSeparatedString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    public static ObjectNode cookieListToObjectNode(List<Cookie> cookieList) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        for (Cookie cookie : cookieList) {
            if (StrUtil.isNotBlank(cookie.value())) {
                objectNode.put(cookie.name(), cookie.value());
            }
        }
        return objectNode;
    }

    @Nonnull
    public static Map<String, String> convertJsonToMap(String json) {
        if (json.isBlank()) {
            return new HashMap<>();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("字符串转json失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public static int parseStrTime(String strTime) {
        // 00:23  01:26
        // 拆分时分秒
        String[] timeParts = strTime.split(":");
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        if (timeParts.length == 2) {
            minutes = Integer.parseInt(timeParts[0]);
            seconds = Integer.parseInt(timeParts[1]);
        } else if (timeParts.length == 3) {
            hours = Integer.parseInt(timeParts[0]);
            minutes = Integer.parseInt(timeParts[1]);
            seconds = Integer.parseInt(timeParts[2]);
        }

        // 计算总秒数
        return (hours * 60 * 60) + (minutes * 60) + seconds;
    }

    public static HttpUrl.Builder getUrlBuilder() {
        HttpUrl parse = HttpUrl.parse(Constant.BAU);
        if (parse != null) {
            return parse.newBuilder();
        } else {
            throw new RuntimeException("错误的bilibili-api链接");
        }
    }
}
