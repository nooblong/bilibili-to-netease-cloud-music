package github.nooblong.download;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusTypeEnum {

    // control by app
    WAIT("等待"),
    PROCESSING("处理中"),
    AUDITING("审核中"),
    // control by 163
    ONLY_SELF_SEE("仅自己可见"),
    ONLINE("已上线"),
    FAILED("失败"),
    TRANSCODE_FAILED("转码失败"),
    // control by app
    OVER_MAX_RETRY("超过重试次数"),
    INTERNAL_ERROR("内部错误"),
    SKIP("跳过"),
    UNKNOWN("未知错误");

    private final String desc;

}
