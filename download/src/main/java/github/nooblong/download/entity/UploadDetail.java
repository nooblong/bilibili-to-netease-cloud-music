package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import github.nooblong.download.StatusTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * @TableName upload_detail
 */
@TableName(value = "upload_detail")
@Data
@Accessors(chain = true)
public class UploadDetail implements Serializable, Comparator<UploadDetail> {
    private Long id;
    private Long subscribeId;
    /**
     * 保存在本地的名称, partName-title-aid-cid.ext
     */
    @TableField(exist = false)
    private String localName;
    private String uploadName;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    private Double offset;
    private Double beginSec;
    private Double endSec;
    private Long voiceId;
    private Long voiceListId;
    private Long privacy;
    private Long retryTimes;
    private StatusTypeEnum status;
    private String bvid;
    private String cid;
    /**
     * 视频原标题, 不含partName
     */
    private String title;
    private Long useVideoCover;
    private Long crack;
    private Long priority;

    @Override
    public int compare(UploadDetail o1, UploadDetail o2) {
        return o1.getPriority().compareTo(o2.getPriority());
    }
}