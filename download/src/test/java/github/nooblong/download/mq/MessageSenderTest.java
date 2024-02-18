package github.nooblong.download.mq;

import github.nooblong.download.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MessageSenderTest extends BaseTest {

    @Autowired
    MessageSender messageSender;

    @Test
    public void sendSth() {
        for (int i = 0; i < 20; i++) {
            messageSender.sendUploadDetailId((long) i, i);
        }
    }

}