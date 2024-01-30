package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
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
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fromTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date toTime;
    private String keyWord;
    private Integer limitSec;
    private String videoOrder;
    private String remark;
    private String netCover;
    private Integer enable;
    private Integer passCheck;
    private Integer useDefaultCover;

    @TableField(exist = false)
    private List<SubscribeReg> subscribeRegs = new ArrayList<>();

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}