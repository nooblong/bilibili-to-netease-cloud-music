package download.bilibili;

import download.BaseTest;
import github.nooblong.download.bilibili.CollectionIterator;
import github.nooblong.download.bilibili.OldCollectionIterator;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.VideoOrder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

class CollectionIteratorTest extends BaseTest {

    @Test
    void next() {
        // collectionId: 1284839
        Iterator<SimpleVideoInfo> upIterator = new CollectionIterator(bilibiliClient, 0, 0,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW,
                "1284839", CollectionVideoOrder.CHANGE,
                new HashMap<>(), -1, "", new AtomicInteger());
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(next.getTitle());
            if (++times >= 40) {
                break;
            }
        }
    }

    @Test
    void nextOld() {
        // collectionId: 1869296
        Iterator<SimpleVideoInfo> upIterator = new OldCollectionIterator(bilibiliClient, 0, 0,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW,
                "1869296", CollectionVideoOrder.DEFAULT,
                new HashMap<>(), -1, "", new AtomicInteger());
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(next.getTitle());
            if (++times >= 1000) {
                break;
            }
        }
    }
}