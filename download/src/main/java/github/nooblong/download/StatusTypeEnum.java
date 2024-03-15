package github.nooblong.download;

public enum StatusTypeEnum {
    //net AUDITING, ONLY_SELF_SEE, ONLINE, FAILED
    WAIT,
    PROCESSING,
    AUDITING,
    ONLY_SELF_SEE,
    ONLINE,
    FAILED,
    OVER_MAX_RETRY,
    INTERNAL_ERROR,
    SKIP,
    TRANSCODE_FAILED,
    UNKNOWN
}
