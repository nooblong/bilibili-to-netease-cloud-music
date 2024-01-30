package github.nooblong.download.netmusic.module.eapi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 关注TA的人(粉丝)
 */
@Service
public class UserFolloweds extends SimpleEApiModule {

    private String uid;

    @Override
    public String getUrl() {
        return "https://music.163.com/eapi/user/getfolloweds/" + this.uid;
    }

    @Override
    public String getOptionsUrl() {
        return "/api/user/getfolloweds";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        uid = (String) queryMap.get("uid");
        node.put("userId", (String) queryMap.get("uid"));
        node.put("time", (String) queryMap.get("0"));
        node.put("limit", StrUtil.nullToDefault((String) queryMap.get("limit"), "30"));
        node.put("offset", StrUtil.nullToDefault((String) queryMap.get("offset"), "0"));
        node.put("getcounts", (String) queryMap.get("true"));
    }
}
