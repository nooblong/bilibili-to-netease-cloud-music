package github.nooblong.btncm;

import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.bilibili.UpIterator;
import github.nooblong.btncm.enums.UserVideoOrderEnum;
import github.nooblong.btncm.enums.VideoOrderEnum;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UpIteratorTest extends BaseTest {

    @Test
    void next() {
        Map<String, String> availableBilibiliCookie = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(1);
        Iterator<SimpleVideoInfo> upIterator = new UpIterator(bilibiliClient, "698029620", "",
                300, 120,
                VideoOrderEnum.PUB_OLD_FIRST_THEN_NEW, UserVideoOrderEnum.PUBDATE, false,
                availableBilibiliCookie, -1, "", counter);
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(counter + ">>>>>" + next.getTitle() + "---->" + next.getPartName());
            if (++times >= 300) {
                break;
            }
        }
    }

}