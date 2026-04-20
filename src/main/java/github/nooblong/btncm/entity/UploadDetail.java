package github.nooblong.btncm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import github.nooblong.btncm.enums.MusicStatusEnum;
import github.nooblong.btncm.enums.UploadStatusTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@TableName(value = "upload_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
//uploadName offset beginSec endSec bitrate voiceListId privacy bvid cid useVideoCover crack
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
    private Integer bitrate;
    private Long voiceId;
    private Long voiceListId;
    private Long privacy;
    private MusicStatusEnum musicStatus;
    private Integer musicRetryTimes;
    private UploadStatusTypeEnum uploadStatus;
    private Integer uploadRetryTimes;
    private String bvid;
    private String cid;
    /**
     * 视频原标题, 不含partName
     */
    private String title;
    private Long useVideoCover;
    private Long crack;
    private Long priority;
    private String log;

    @TableField(exist = false)
    private String voiceListName;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String statusDesc;
    @TableField(exist = false)
    private String mergeTitle;
    @TableField(exist = false)
    private String subscribeName;
    @TableField(exist = false)
    private List<CidName> cidNames;

    @Override
    public int compareTo(@NotNull UploadDetail o) {
        if (o.getPriority().equals(this.getPriority())) {
            return this.getId().compareTo(o.getId());
        }
        return o.getPriority().compareTo(this.getPriority());
    }
}
