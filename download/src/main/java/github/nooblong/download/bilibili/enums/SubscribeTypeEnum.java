package github.nooblong.download.bilibili.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscribeTypeEnum {
    UP("UP主"),
    COLLECTION("合集(新)"),
    FAVORITE("收藏夹"),
    PART("多p视频");

    private final String desc;
}
