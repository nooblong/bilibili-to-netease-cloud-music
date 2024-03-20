package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import github.nooblong.download.StatusTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * @TableName upload_detail
 */
@TableName(value = "upload_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UploadDetail implements Serializable, Comparable<UploadDetail> {
    private Long id;
    private Long subscribeId;
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
    private Integer retryTimes;
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
    private Long jobId;
    private String log;

    @Override
    public int compareTo(@NotNull UploadDetail o) {
        return this.getPriority().compareTo(o.getPriority());
    }
}