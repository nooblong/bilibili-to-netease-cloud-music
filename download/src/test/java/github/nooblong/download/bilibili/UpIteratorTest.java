package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.VideoOrder;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class UpIteratorTest extends BaseTest {

    @Test
    void next() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        AtomicInteger counter = new AtomicInteger(1);
        Iterator<SimpleVideoInfo> upIterator = new UpIterator(bilibiliClient, "78504036", "", 99999,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW, UserVideoOrder.PUBDATE, false,
                availableBilibiliCookie, -1, "", counter);
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(counter + ">>>>>" + next.getTitle() + "---->" + next.getPartName());
            if (++times >= 1100) {
                break;
            }
        }
    }

}