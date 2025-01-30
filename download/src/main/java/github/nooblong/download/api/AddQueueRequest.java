package github.nooblong.download.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AddQueueRequest {
    String bvid;
    List<String> cid;
    String uploadName;
    Long voiceListId;
    Boolean useDefaultImg;
    Double voiceOffset;
    Double voiceBeginSec;
    Double voiceEndSec;
    Boolean privacy;
    Boolean crack;
}
