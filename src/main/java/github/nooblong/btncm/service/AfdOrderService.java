package github.nooblong.btncm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.entity.AfdOrder;

public interface AfdOrderService extends IService<AfdOrder> {

    /**
     * 爱发电生成订单
     */
    String generateOrder(Long userId);

    /**
     * 爱发电更新订单
     */
    void updateData();

    /**
     * 爱发电更新用户
     */
    void updateUser();
}
