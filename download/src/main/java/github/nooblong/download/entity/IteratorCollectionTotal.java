package github.nooblong.download.entity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IteratorCollectionTotal {
    private ArrayNode data;
    private int totalNum;
}
