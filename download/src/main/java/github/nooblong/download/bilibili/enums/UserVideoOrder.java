package github.nooblong.download.bilibili.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum UserVideoOrder {
    PUBDATE("pubdate"),
    FAVORITE("stow"),
    VIEW("click"),
    ;

    final String value;

    public static final Map<String, String> map = new HashMap<>();
    public static final Map<String, String> extMap = new HashMap<>();

    static {
        for (UserVideoOrder videoOrder : UserVideoOrder.values()) {
            map.put(videoOrder.name(), videoOrder.getValue());
        }
        for (UserVideoOrder videoOrder : UserVideoOrder.values()) {
            extMap.put(videoOrder.getValue(), videoOrder.name());
        }
    }
}
