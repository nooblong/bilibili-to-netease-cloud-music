package github.nooblong.btncm.enums;

public enum VideoOrderEnum {
    /**
     * 先上传新的视频
     * 先上传以前发布的视频
     */
    PUB_NEW_FIRST_THEN_OLD(),
    PUB_OLD_FIRST_THEN_NEW(),
    ;

    VideoOrderEnum() {
    }

}
