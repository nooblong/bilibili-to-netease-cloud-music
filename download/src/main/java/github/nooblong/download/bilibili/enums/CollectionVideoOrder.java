package github.nooblong.download.bilibili.enums;

import lombok.Getter;

@Getter
// 暂时无用,固定CHANGE
public enum CollectionVideoOrder {
//    合集视频排序顺序。
//    + DEFAULT: 默认排序
//    + CHANGE : 升序排序

    DEFAULT("false"),
    CHANGE("true"),
    ;

    final String value;

    CollectionVideoOrder(String value) {
        this.value = value;
    }
}
