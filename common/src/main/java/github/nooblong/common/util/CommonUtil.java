package github.nooblong.common.util;

import ch.qos.logback.classic.Level;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommonUtil {

    public static void debug() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.DEBUG);
        logger.debug("debug mode");
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
        return input.length() > 20 ? input.substring(0, 20) : input;
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
}
