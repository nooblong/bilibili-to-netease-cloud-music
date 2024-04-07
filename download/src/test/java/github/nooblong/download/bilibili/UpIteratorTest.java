package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

class UpIteratorTest extends BaseTest {

    @Autowired
    BilibiliBatchIteratorFactory factory;

    @Test
    void next() {
        Iterator<SimpleVideoInfo> upIterator = factory.createUpIterator("349032426", "", 300, true,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW, UserVideoOrder.PUBDATE);
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(">>>>>" + next.getTitle() + "---->" + next.getPartName());
            if (++times >= 100) {
                break;
            }
        }
    }
}