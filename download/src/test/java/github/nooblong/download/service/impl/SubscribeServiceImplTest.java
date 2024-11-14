package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeServiceImplTest extends BaseTest {

    @Autowired
    private SubscribeServiceImpl subscribeService;

    @Test
    void subscribe() {
        subscribeService.checkAndSave();
    }

}