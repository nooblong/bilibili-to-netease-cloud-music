package github.nooblong.download.api;

import github.nooblong.download.entity.Subscribe;
import lombok.Data;

import java.util.List;

@Data
public class SubscribeResponse {
    Long targetId;
    List<Subscribe> subscribeList;
}
