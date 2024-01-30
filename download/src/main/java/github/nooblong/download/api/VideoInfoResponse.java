package github.nooblong.download.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VideoInfoResponse {
    private String title;
    private String image;
    private String quality;
    private JsonNode pages;
    private String author;
    private String uid;
}
