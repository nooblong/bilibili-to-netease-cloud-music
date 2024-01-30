package github.nooblong.download.mq;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VideoMessage {
    private String url;
    private String cid;
    private String title;
    private Long uploadUserId;
    private Long uploadVoiceListId;
    private Long crack;
    private Long cut;
    private Double voiceBeginSec;
    private Double voiceEndSec;
    private Double voiceOffset;
    private String customUploadName;
}
