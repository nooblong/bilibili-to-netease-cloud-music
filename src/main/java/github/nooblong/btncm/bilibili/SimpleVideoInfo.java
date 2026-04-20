package github.nooblong.btncm.bilibili;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 单个视频的简单信息，可能只包含bvid
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
