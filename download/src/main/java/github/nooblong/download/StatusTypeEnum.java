package github.nooblong.download;

public enum StatusTypeEnum {
    // control by app
    WAIT,
    PROCESSING,
    AUDITING,
    // control by 163
    ONLY_SELF_SEE,
    ONLINE,
    FAILED,
    TRANSCODE_FAILED,
    // control by app
    OVER_MAX_RETRY,
    INTERNAL_ERROR,
    SKIP,
    UNKNOWN
}
