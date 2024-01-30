package github.nooblong.download.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddQueueRequest {
    @NotBlank
    String bvid;
    String cid;
    String customUploadName;
    @Min(0)
    Long voiceListId;
    boolean useDefaultImg;
    double voiceOffset;
    double voiceBeginSec;
    double voiceEndSec;
    boolean privacy;
    boolean crack;
    boolean move;
}
