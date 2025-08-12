package download.service.impl;

import download.BaseTest;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.service.impl.SubscribeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class SubscribeServiceImplTest extends BaseTest {

    @Autowired
    private SubscribeServiceImpl subscribeService;

    @Test
    void subscribe() throws InterruptedException {
        subscribeService.checkAndSave();
        System.out.println("执行完成...");
        Thread.sleep(1000000);
    }

    @Test
    void nextFromSubscribe() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        Subscribe subscribe = subscribeService.getById(666);
        subscribeService.checkSubscribe(subscribe, availableBilibiliCookie);
    }

}