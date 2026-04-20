package github.nooblong.btncm.bilibili;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class VideoInfoResponse implements Serializable {
    private String title;
    private String image;
    private String quality;
    private JsonNode pages;
    private String author;
    private String uid;
}
