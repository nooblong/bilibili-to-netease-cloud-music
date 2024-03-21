package github.nooblong.download.mq;

import github.nooblong.download.BaseTest;
import github.nooblong.download.entity.UploadDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MusicQueueTest extends BaseTest {

    @Test
    void listWorkersAddrAvailable() {
        List<String> strings = musicQueue.listWorkersAddrAvailable();
        System.out.println(strings);
    }

    @Test
    void forAddQueue() throws InterruptedException {
        List<UploadDetail> list = uploadDetailService.list();
        for (int i = 0; i < 10; i++) {
            musicQueue.enQueue(list.get(i));
        }
        Thread.sleep(1000000000);
    }
}