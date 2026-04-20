package github.nooblong.btncm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MusicStatusEnum {

    WAIT("等待"),
    AUDITING("审核中"),
    SCHEDULE_PUBLISH("定时发布中"),
    PUBLISHING("发布中"),
    ONLY_SELF_SEE("仅自己可见"),
    ONLINE("已上线"),
    FAILED("发布失败"),
    TRANSCODE_FAILED("转码失败"),
    UNKNOWN("未知"),
    NO_COOKIE("没有Cookie"),

    ;
    private final String desc;

}
