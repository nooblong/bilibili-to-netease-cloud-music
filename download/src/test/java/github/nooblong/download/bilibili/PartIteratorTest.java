package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.VideoOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;

class PartIteratorTest extends BaseTest {

    @Test
    void next() {
        Iterator<SimpleVideoInfo> partIterator = new PartIterator(bilibiliClient, 99999,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW, "BV1FQ4y1z7eD",
                new HashMap<>());
        int times = 0;
        while (partIterator.hasNext()) {
            SimpleVideoInfo next = partIterator.next();
            System.out.println(next.getPartName());
            if (++times >= 1000) {
                break;
            }
        }
    }
}