package github.nooblong.download.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.entity.AfdOrder;

public interface AfdOrderService extends IService<AfdOrder> {

    String generateOrder(Long userId);

    void updateData();

    void updateUser();
}
