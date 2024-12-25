package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeServiceImplTest extends BaseTest {

    @Autowired
    private SubscribeServiceImpl subscribeService;

    @Test
    void subscribe() throws InterruptedException {
        subscribeService.checkAndSave();
        System.out.println("执行完成...");
        Thread.sleep(1000000);
    }

}