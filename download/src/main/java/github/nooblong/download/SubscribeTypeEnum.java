package github.nooblong.download;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscribeTypeEnum {
    UP("UP主"),
    COLLECTION("合集(新)"),
    OLDCOLLECTION("合集(旧)-视频列表"),
    FAVORITE("收藏夹"),
    PART("多p视频")
    ;

    private final String desc;
}
