package github.nooblong.download.bilibili;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.Assert;

import java.util.Date;

@Data
@Accessors(chain = true)
public class BilibiliFullVideo {
    private JsonNode videoInfo;
    private String selectCid;

    public String getBvid() {
        return videoInfo.get("data").get("bvid").asText();
    }

    public String getPartName() {
        if (selectCid == null) {
            JsonNode pages = videoInfo.get("data").get("pages");
            return pages.get(0).get("part").asText();
        }
        JsonNode pages = videoInfo.get("data").get("pages");
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).get("cid").asText().equals(selectCid)) {
                return pages.get(i).get("part").asText();
            }
        }
        return null;
    }

    public String getCid() {
        return selectCid != null ? selectCid : videoInfo.get("data").get("pages").get(0).get("cid").asText();
    }

    public String getTitle() {
        return videoInfo.get("data").get("title").asText();
    }

    public Date getVideoCreateTime() {
        long time = videoInfo.get("data").get("pubdate").asLong();
        return new Date(time * 1000);
    }

    public String getAuthor() {
        return videoInfo.get("data").get("owner").get("name").asText();
    }

    public String getUserId() {
        return videoInfo.get("data").get("owner").get("mid").asText();
    }

    public boolean getHasMultiPart() {
        return videoInfo.get("data").has("pages") && getVideoInfo().get("data").get("pages").size() > 1;
    }

    public ArrayNode getPartVideos() {
        return (ArrayNode) videoInfo.get("data").get("pages");
    }

    public int getPartNumbers() {
        return videoInfo.get("data").get("videos").asInt();
    }

    public boolean getHasSeries() {
        return videoInfo.get("data").has("ugc_season")
                && getVideoInfo().get("data").get("ugc_season").has("sections")
                && !getVideoInfo().get("data").get("ugc_season").get("sections").isEmpty()
                && getVideoInfo().get("data").get("ugc_season").get("sections").get(0).has("episodes");
    }

    public String getMySeriesId() {
        Assert.isTrue(getHasSeries(), "没有合集的视频哪来的id");
        return getVideoInfo().get("data").get("ugc_season").get("id").asText();
    }

    public int getDuration() {
        return videoInfo.get("data").get("duration").asInt();
    }
}
