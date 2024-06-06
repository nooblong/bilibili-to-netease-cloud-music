package github.nooblong.common.util;

import ch.qos.logback.classic.Level;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (lines.length <= 100) {
            return input; // 不超过100行，返回全部字符串
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = lines.length - 99; i < lines.length; i++) {
                result.append(lines[i]).append("\n");
            }
            return result.toString();
        }
    }

}
