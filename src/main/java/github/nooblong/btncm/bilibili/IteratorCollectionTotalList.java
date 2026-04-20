package github.nooblong.btncm.bilibili;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 帮助类
 */
@Data
@Accessors(chain = true)
public class IteratorCollectionTotalList<T> implements Serializable {
    private List<T> data;
    private int totalNum;
}
