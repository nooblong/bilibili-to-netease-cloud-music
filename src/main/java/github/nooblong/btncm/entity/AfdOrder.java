package github.nooblong.btncm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;


@TableName(value = "afd_order")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class AfdOrder implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderId;

    private Long userId;

    private String outUserId;

    private String planId;

    private String title; // 订单描述

    private String month; // 赞助月份

    private String totalAmount; // 真实付款金额，如有兑换码，则为0.00

    private String showAmount; // 显示金额，如有折扣则为折扣前金额

    private Integer status;

    private String remark;

    private String redeemId; // 兑换码ID

    private Integer productType; // 0表示常规方案 1表示售卖方案

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date outCreateTime;
}
