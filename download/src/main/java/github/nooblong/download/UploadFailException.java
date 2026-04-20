package github.nooblong.download;

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
