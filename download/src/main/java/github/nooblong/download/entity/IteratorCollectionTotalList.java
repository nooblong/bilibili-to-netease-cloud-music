package github.nooblong.download.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class IteratorCollectionTotalList<T> {
    private List<T> data;
    private int totalNum;
}
