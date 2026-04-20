package github.nooblong.btncm;

import github.nooblong.btncm.bilibili.FavoriteIterator;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;

class FavoriteIteratorTest extends BaseTest {

    @Test
    void next() {
        Iterator<SimpleVideoInfo> upIterator = new FavoriteIterator("68629352", bilibiliClient,
                9999, 0, true, new HashMap<>());
        int times = 0;
        while (upIterator.hasNext()) {
            SimpleVideoInfo next = upIterator.next();
            System.out.println(next.getTitle());
            if (++times >= 100) {
                break;
            }
        }
    }

}