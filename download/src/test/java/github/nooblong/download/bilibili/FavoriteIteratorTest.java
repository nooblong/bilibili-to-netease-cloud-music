package github.nooblong.download.bilibili;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.enums.VideoOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

class FavoriteIteratorTest extends BaseTest {

    @Autowired
    BilibiliBatchIteratorFactory factory;

    @Test
    void next() {
        Iterator<BilibiliVideo> upIterator = factory.createFavoriteIterator("2698957987",
                VideoOrder.PUB_NEW_FIRST_THEN_OLD, 9999);
        int times = 0;
        while (upIterator.hasNext()) {
            BilibiliVideo next = upIterator.next();
            System.out.println(next.getTitle());
            if (++times >= 100) {
                break;
            }
        }
    }

}