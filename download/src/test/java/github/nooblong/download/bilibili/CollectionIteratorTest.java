package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.CollectionVideoOrder;
import github.nooblong.download.bilibili.enums.VideoOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;

class CollectionIteratorTest extends BaseTest {

    @Autowired
    BilibiliBatchIteratorFactory factory;

    @Test
    void next() {
        // collectionId: 1284839
        Iterator<SimpleVideoInfo> upIterator = factory.createCollectionIterator("1284839", 9999,
                VideoOrder.PUB_OLD_FIRST_THEN_NEW, CollectionVideoOrder.CHANGE, new HashMap<>());
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(next.getTitle());
            if (++times >= 40) {
                break;
            }
        }
    }
}