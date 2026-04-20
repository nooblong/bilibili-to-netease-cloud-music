package github.nooblong.btncm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.entity.AfdOrder;

public interface AfdOrderService extends IService<AfdOrder> {

    String generateOrder(Long userId);

    void updateData();

    void updateUser();
}
