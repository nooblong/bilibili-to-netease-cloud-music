package github.nooblong.download.bilibili.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum AudioQuality {
    _64K(30216, "m4a"),
    _132K(30232, "m4a"),
    DOLBY(30250, "m4a"),
    HI_RES(30251, "flac"),
    _192K(30280, "m4a"),
    ;

    final int code;
    final String ext;

    public static final Map<Integer, AudioQuality> extMap = new HashMap<>();
    public static final Map<Integer, AudioQuality> descMap = new HashMap<>();

    static {
        for (AudioQuality audioQuality : AudioQuality.values()) {
            extMap.put(audioQuality.getCode(), audioQuality);
        }
        for (AudioQuality audioQuality : AudioQuality.values()) {
            descMap.put(audioQuality.getCode(), audioQuality);
        }
    }

}
