package github.nooblong.download.bilibili;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * 单个视频
 */
@Data
@Accessors(chain = true)
@ToString(doNotUseGetters = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BilibiliVideo implements Serializable {
    private String bvid;
    private String cid;
    private JsonNode videoInfo;
    // 在没有videoInfo时使用
    private Integer duration;
    private String title;
    private String partName;
    private Long createTime;

    public BilibiliVideo setBvid(String bvid) {
        Assert.isTrue(!bvid.startsWith("http") &&
                !bvid.startsWith("www.") &&
                !bvid.startsWith("b23.tv") &&
                !bvid.startsWith("bilibili"), "只能是纯正bvid");
        this.bvid = bvid;
        return this;
    }

    @JsonIgnore
    public String getCidOrDefault() {
        return getCid() != null ? cid : getDefaultCid();
    }

    @JsonIgnore
    public String getDefaultCid() {
        return videoInfo.get("data").get("pages").get(0).get("cid").asText();
    }

    public String getUploadName() {
        if (cid != null) {
            if (title != null) {
                Assert.notNull(partName, "分p视频title需要partName");
                return partName + "-" + title;
            } else {
                Assert.notNull(getVideoInfo(), "没有预先赋值需要初始化先");
                return getPartName() + "-" + getVideoInfo().get("data").get("title").asText();
            }
        }
        if (title != null) {
            return title;
        }
        return getVideoInfo() != null ? getVideoInfo().get("data").get("title").asText() : null;
    }

    public String getTitle() {
        if (title != null) {
            return title;
        }
        return getVideoInfo() != null ? getVideoInfo().get("data").get("title").asText() : null;
    }

    public Date getVideoCreateTime() {
        if (createTime != null) {
            return new Date(createTime * 1000);
        }
        if (getVideoInfo() == null) {
            return null;
        }
        long time = getVideoInfo().get("data").get("pubdate").asLong();
        return new Date(time * 1000);
    }

    public String getPartName() {
        if (cid == null) {
            return null;
        }
        if (partName != null) {
            return partName;
        }
        JsonNode pages = getVideoInfo().get("data").get("pages");
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).get("cid").asText().equals(cid)) {
                return pages.get(i).get("part").asText();
            }
        }
        return null;
    }

    @JsonIgnore
    public String getAuthor() {
        return getVideoInfo().get("data").get("owner").get("name").asText();
    }

    @JsonIgnore
    public String getUserId() {
        return getVideoInfo().get("data").get("owner").get("mid").asText();
    }

    @JsonIgnore
    public String getLocalName() {
        return assembleLocalName(getPartName(), getTitle(), getBvid(), getCid());
    }

    @JsonIgnore
    public String getLocalCoverName() {
        return assembleLocalName(getPartName(), getTitle(), getBvid(), getCid());
    }

    @JsonIgnore
    public boolean getHasMultiPart() {
        return getVideoInfo().get("data").has("pages") && getVideoInfo().get("data").get("pages").size() > 1;
    }

    @JsonIgnore
    public ArrayNode getPartVideos() {
        return (ArrayNode) getVideoInfo().get("data").get("pages");
    }

    @JsonIgnore
    public int getPartNumbers() {
        return getVideoInfo().get("data").get("videos").asInt();
    }

    @JsonIgnore
    public boolean getHasSeries() {
        return getVideoInfo().get("data").has("ugc_season")
                && getVideoInfo().get("data").get("ugc_season").has("sections")
                && !getVideoInfo().get("data").get("ugc_season").get("sections").isEmpty()
                && getVideoInfo().get("data").get("ugc_season").get("sections").get(0).has("episodes");
    }

    @JsonIgnore
    public String getMySeriesId() {
        assert getHasSeries();
        return getVideoInfo().get("data").get("ugc_season").get("id").asText();
    }

    @JsonIgnore
    private static String assembleLocalName(String partName, String videoName, String bvid, String cid) {
        // [partName(null)]-[videoName]-[bvid]-[cid(null)]
        if (videoName.length() > 30) {
            videoName = videoName.substring(0, 30);
        }
        String sb = "[" + partName + "]" + "-" +
                "[" + videoName + "]" + "-" +
                "[" + bvid + "]" + "-" +
                "[" + cid + "]";
        return sb.replaceAll("/", "");
    }

    @JsonIgnore
    public int getDuration() {
        return Objects.requireNonNullElseGet(duration, () -> getVideoInfo().get("data").get("duration").asInt());
    }
}
