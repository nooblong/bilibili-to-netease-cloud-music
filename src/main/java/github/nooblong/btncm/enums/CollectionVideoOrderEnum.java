package github.nooblong.btncm.enums;

import lombok.Getter;

@Getter
public enum CollectionVideoOrderEnum {
//    合集视频排序顺序。
//    + DEFAULT: 默认排序
//    + CHANGE : 升序排序

    DEFAULT("false"),
    CHANGE("true"),
    ;

    final String value;

    CollectionVideoOrderEnum(String value) {
        this.value = value;
    }
}
