package github.nooblong.btncm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum CodecEnum {
    /**
     * 值	含义	备注
     * 7	AVC 编码	8K 视频不支持该格式
     * 12	HEVC 编码
     * 13	AV1 编码
     **/
    AVC(7, "h.264"),
    HEVC(12, "h.265"),
    AV1(13, "av1"),
    ;

    final int code;
    final String ext;

    public static final Map<Integer, CodecEnum> codeMap = new HashMap<>();

    static {
        for (CodecEnum codecEnum : CodecEnum.values()) {
            codeMap.put(codecEnum.getCode(), codecEnum);
        }
    }

}
