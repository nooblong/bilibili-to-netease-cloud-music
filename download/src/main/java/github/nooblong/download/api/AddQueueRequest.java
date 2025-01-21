package github.nooblong.download.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddQueueRequest {
    String bvid;
    String cid;
    String uploadName;
    Long voiceListId;
    Boolean useDefaultImg;
    Double voiceOffset;
    Double voiceBeginSec;
    Double voiceEndSec;
    Boolean privacy;
    Boolean crack;
}
