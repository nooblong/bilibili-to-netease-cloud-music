package github.nooblong.btncm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum VideoQualityEnum {
    /**
     * 视频清晰度码率对照
     * 6   = 240P 极速     仅 MP4 格式支持，仅 platform=html5 时有效
     * 16  = 360P 流畅
     * 32  = 480P 清晰
     * 64  = 720P 高清     WEB 端默认值，无 720P 时则为 720P60
     * 74  = 720P60 高帧率  登录认证
     * 80  = 1080P 高清    TV 端与 APP 端默认值，登录认证
     * 100 = 智能修复       大会员认证
     * 112 = 1080P+ 高码率  大会员认证
     * 116 = 1080P60 高帧率 大会员认证
     * 120 = 4K 超清       需 fnval&128=128 且 fourk=1，大会员认证
     * 125 = HDR 真彩色     仅支持 DASH 格式，需 fnval&64=64，大会员认证
     * 126 = 杜比视界       仅支持 DASH 格式，需 fnval&512=512，大会员认证
     * 127 = 8K 超高清      仅支持 DASH 格式，需 fnval&1024=1024，大会员认证
     * 129 = HDR Vivid    大会员认证
     */
    P240(6, "mp4"),
    P360(16, "mp4"),
    P480(32, "mp4"),
    P720(64, "mp4"),
    P720_60(74, "mp4"),
    P1080(80, "mp4"),
    AI_REPAIR(100, "mp4"),
    P1080_PLUS(112, "mp4"),
    P1080_60(116, "mp4"),
    K4(120, "mp4"),
    HDR(125, "mp4"),
    DOLBY_VISION(126, "mp4"),
    K8(127, "mp4"),
    HDR_VIVID(129, "mp4"),
    ;

    final int code;
    final String ext;

    public static final Map<Integer, VideoQualityEnum> codeMap = new HashMap<>();

    static {
        for (VideoQualityEnum videoQuality : VideoQualityEnum.values()) {
            codeMap.put(videoQuality.getCode(), videoQuality);
        }
    }

}
