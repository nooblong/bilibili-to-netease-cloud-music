package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import github.nooblong.download.bilibili.enums.SubscribeTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @TableName subscribe
 */
@TableName(value = "subscribe")
@Data
public class Subscribe implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String regName;
    private Long userId;
    private Long voiceListId;
    private String targetId;
    private SubscribeTypeEnum type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date fromTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date toTime;
    private String keyWord;
    private Integer limitSec;
    private String videoOrder;
    private String remark;
    private String netCover;
    private Integer enable;
    private Integer crack;
    private Integer useVideoCover;
    private Integer priority;
    private String log;

    @TableField(exist = false)
    private List<SubscribeReg> subscribeRegs = new ArrayList<>();
    @TableField(exist = false)
    private String typeDesc;

}