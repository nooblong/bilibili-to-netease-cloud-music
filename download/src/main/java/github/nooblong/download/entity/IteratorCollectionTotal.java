package github.nooblong.download.entity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class IteratorCollectionTotal implements Serializable {
    private ArrayNode data;
    private int totalNum;
}
