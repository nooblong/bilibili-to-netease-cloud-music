package github.nooblong.btncm.bilibili;

import github.nooblong.btncm.enums.UploadStatusTypeEnum;

/**
 * 上传失败异常类
 */
public class UploadFailException extends Exception {

    private final UploadStatusTypeEnum uploadStatusType;

    public UploadFailException(UploadStatusTypeEnum uploadStatusTypeEnum) {
        super(uploadStatusTypeEnum.getDesc());
        this.uploadStatusType = uploadStatusTypeEnum;
    }

    public UploadStatusTypeEnum getuploadStatusTypeEnum() {
        return uploadStatusType;
    }
}
