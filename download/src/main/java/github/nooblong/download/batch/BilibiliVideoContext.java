package github.nooblong.download.batch;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class BilibiliVideoContext implements Serializable {
    // copyProperties
    private String bvid;
    private String cid;
    private Integer duration;
    private String title;
    private String partName;
    private String author;
    // 默认title加partName,不受自定义影响
    private String uploadName;


    // 手动赋值
    // in UploadSingleAudioGetDataStep
    private String path;
    private String imagePath;
    private List<String> descList;

    // in UploadSingleAudioCutImageStep
    private String netImageId;

    // in UploadSingleAudioUploadMusicStep
    private String voiceId;
}
