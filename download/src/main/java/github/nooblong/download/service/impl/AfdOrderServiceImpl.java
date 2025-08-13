package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.AfdUtil;
import github.nooblong.download.entity.AfdOrder;
import github.nooblong.download.mapper.AfdOrderMapper;
import github.nooblong.download.service.AfdOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AfdOrderServiceImpl extends ServiceImpl<AfdOrderMapper, AfdOrder> implements AfdOrderService {

    final AfdUtil afdUtil;

    public AfdOrderServiceImpl(AfdUtil afdUtil) {
        this.afdUtil = afdUtil;
    }


    @Override
    public String generateOrder(Long userId) {
        int timestampInt = (int) (System.currentTimeMillis() / 1000);
        AfdOrder order = new AfdOrder();
        order.setUserId(userId);
        order.setCreateTime(new Date());
        order.setOrderId(String.valueOf(timestampInt));
        save(order);
        return order.getOrderId();
    }

    @Override
    public void updateData() {
        JsonNode send = afdUtil.reqOrder();
        for (JsonNode jsonNode : send) {
            if (jsonNode.has("custom_order_id")) {
                String customOrderId = jsonNode.get("custom_order_id").asText();
                AfdOrder order = getOne(Wrappers.<AfdOrder>lambdaQuery().eq(AfdOrder::getOrderId, customOrderId));
                if (order != null && order.getOutUserId() == null) {
                    order.setOutUserId(jsonNode.get("user_id").asText());
                    order.setMonth(jsonNode.get("month").asText());
                    order.setRemark(jsonNode.get("remark").asText());
                    order.setPlanId(jsonNode.get("plan_id").asText());
                    order.setProductType(jsonNode.get("product_type").asInt());
                    order.setTotalAmount(jsonNode.get("total_amount").asText());
                    order.setShowAmount(jsonNode.get("show_amount").asText());
                    order.setRedeemId(jsonNode.get("redeem_id").asText());
                    order.setStatus(jsonNode.get("status").asInt());
                    order.setOutCreateTime(new Date(jsonNode.get("create_time").asLong() * 1000));
                    updateById(order);

                    String outUserId = jsonNode.get("user_id").asText();
                    SysUser user = Db.getById(order.getUserId(), SysUser.class);
                    user.setAfdUserId(outUserId);
                    Db.updateById(user);
                } else {
                    log.info("未知的orderId");
                }
            } else {
                log.info("未知订阅者");
            }
        }
    }

    @Override
    public void updateUser() {
        JsonNode send = afdUtil.reqUser();
        for (JsonNode jsonNode : send) {
            String outUserId = jsonNode.get("user").get("user_id").asText();
            SysUser sysUser = Db.getOne(Wrappers.lambdaQuery(SysUser.class)
                    .eq(SysUser::getAfdUserId, outUserId));
            if (sysUser != null) {
                JsonNode currentPlan = jsonNode.get("current_plan");
                if (currentPlan.has("name") &&
                        StrUtil.isNotBlank(currentPlan.get("name").asText())) {
                    long expireTime = currentPlan.get("expire_time").asLong();
                    Date expireDate = new Date(expireTime * 1000);
                    sysUser.setExpire(expireDate);
                    sysUser.setTotalPay(jsonNode.get("all_sum_amount").asText());
                    Db.updateById(sysUser);
                }
            } else {
                log.info("未知的outUserId");
            }
        }
    }
}
