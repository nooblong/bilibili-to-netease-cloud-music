package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.VideoOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

class PartIteratorTest extends BaseTest {

    @Autowired
    BilibiliBatchIteratorFactory factory;

    @Test
    void next() {
        Iterator<BilibiliVideo> partIterator = factory.createPartIterator("BV1FQ4y1z7eD",
                VideoOrder.PUB_OLD_FIRST_THEN_NEW, 9999);
        int times = 0;
        while (partIterator.hasNext()) {
            BilibiliVideo next = partIterator.next();
            System.out.println(next.getPartName());
            if (++times >= 1000) {
                break;
            }
        }
    }
}