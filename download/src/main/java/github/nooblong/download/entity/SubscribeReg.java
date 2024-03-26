package github.nooblong.download.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName subscribe_reg
 */
@TableName(value = "subscribe_reg")
@Data
public class SubscribeReg implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subscribeId;
    private String regex;
    private Integer pos;

}