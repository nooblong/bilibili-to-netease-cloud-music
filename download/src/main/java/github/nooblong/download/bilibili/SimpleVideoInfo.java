package github.nooblong.download.bilibili;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 单个视频
 */
@Data
@Accessors(chain = true)
public class SimpleVideoInfo implements Serializable {
    private String bvid;
    private String cid;
    private Integer duration;
    private String title;
    private String partName;
    private Long createTime;
}
