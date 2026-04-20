package github.nooblong.btncm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum AudioQualityEnum {
    /**
     * _64K = 30216
     * _132K = 30232
     * DOLBY = 30255
     * HI_RES = 30251
     * _192K = 30280
     **/
    _64K(30216, "m4a"),
    _132K(30232, "m4a"),
    DOLBY(30255, "m4a"),
    HI_RES(30251, "flac"),
    _192K(30280, "m4a"),
    ;

    final int code;
    final String ext;

    public static final Map<Integer, AudioQualityEnum> extMap = new HashMap<>();
    public static final Map<Integer, AudioQualityEnum> descMap = new HashMap<>();

    static {
        for (AudioQualityEnum audioQuality : AudioQualityEnum.values()) {
            extMap.put(audioQuality.getCode(), audioQuality);
        }
        for (AudioQualityEnum audioQuality : AudioQualityEnum.values()) {
            descMap.put(audioQuality.getCode(), audioQuality);
        }
    }

}
