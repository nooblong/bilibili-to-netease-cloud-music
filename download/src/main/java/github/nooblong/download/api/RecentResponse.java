package github.nooblong.download.api;

import lombok.Data;

@Data
public class RecentResponse {
    private String id;
    private String name;
    private String createTime;
    private String userName;
    private String displayStatus;
    private String voiceId;
    private String voiceListId;
}
