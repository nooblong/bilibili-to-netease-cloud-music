package github.nooblong.btncm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum UserVideoOrderEnum {
    PUBDATE("pubdate"),
    FAVORITE("stow"),
    VIEW("click"),
    ;

    final String value;

    public static final Map<String, String> map = new HashMap<>();
    public static final Map<String, String> extMap = new HashMap<>();

    static {
        for (UserVideoOrderEnum videoOrder : UserVideoOrderEnum.values()) {
            map.put(videoOrder.name(), videoOrder.getValue());
        }
        for (UserVideoOrderEnum videoOrder : UserVideoOrderEnum.values()) {
            extMap.put(videoOrder.getValue(), videoOrder.name());
        }
    }
}
