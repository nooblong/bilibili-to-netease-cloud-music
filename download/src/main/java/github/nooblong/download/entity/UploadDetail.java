package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @TableName upload_detail
 */
@TableName(value = "upload_detail")
@Data
@Accessors(chain = true)
public class UploadDetail implements Serializable {
    private Long id;
    private Long subscribeId;
    /**
     * 保存在本地的名称, partName-title-aid-cid.ext
     */
    private String localName;
    private String uploadName;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    private Double voiceOffset;
    private Double videoBeginSec;
    private Double videoEndSec;
    private Long voiceId;
    private Long voiceListId;
    private Long privacy;
    /**
     * AUDITING, ONLY_SELF_SEE, ONLINE, FAILED
     */
    private String displayStatus;
    private Long getDisplayStatusTimes;
    private String videoInfo;
    private Long retryTimes;
    private String status;
    private String bvid;
    private String cid;
    /**
     * 视频原标题, 不含partName
     */
    private String title;
    private Long useVideoCover;
    private Long crack;
    @Serial
    private static final long serialVersionUID = 1L;
}