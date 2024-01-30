package github.nooblong.download.api;

import lombok.Data;

@Data
public class DataResponse {

    private long registerNum;
    private long addQueueNum;
    private long diskUseNum;
    private long downloadFileNum;
    private long sysFreeNum;

}
