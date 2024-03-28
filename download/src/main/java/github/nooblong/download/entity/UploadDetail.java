package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import github.nooblong.download.StatusTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
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
    @JsonFormat(pattern = "MM-dd HH:mm", timezone="GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "MM-dd HH:mm", timezone="GMT+8")
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;
    private String log;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String statusDesc;

    @Override
    public int compareTo(@NotNull UploadDetail o) {
        return this.getPriority().compareTo(o.getPriority());
    }
}