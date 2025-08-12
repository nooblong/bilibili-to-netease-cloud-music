package download.bilibili;

import download.BaseTest;
import github.nooblong.download.VideoOrder;
import github.nooblong.download.bilibili.PartIterator;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;

class PartIteratorTest extends BaseTest {

    @Test
    void next() {
        Iterator<SimpleVideoInfo> partIterator = new PartIterator(bilibiliClient, 99999, 0,
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