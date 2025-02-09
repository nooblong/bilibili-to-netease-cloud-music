package github.nooblong.download;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UploadStatusTypeEnum {

    WAIT("等待"),
    PROCESSING("处理中"),
    MAX_RETRY("超过最大处理次数"),
    SUCCESS("成功"),
    ERROR("失败");

    private final String desc;

}
