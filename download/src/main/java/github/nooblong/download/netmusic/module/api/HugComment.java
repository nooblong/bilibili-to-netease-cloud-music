package github.nooblong.download.netmusic.module.api;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.TypeConfig;
import github.nooblong.download.netmusic.module.base.SimpleApiModule;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author wjning
 * @date 2021/6/7 16:43
 */
@Service
public class HugComment extends SimpleApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        String type = TypeConfig.resourceTypeMap.get(StrUtil.nullToDefault((String) queryMap.get("type"), "0"));
        String threadId = type + queryMap.get("sid");

        node.put("targetUserId", (String) queryMap.get("uid"));
        node.put("commentId", (String) queryMap.get("cid"));
        node.put("threadId", threadId);
    }

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("ios").build());
        cookieList.add(OkUtil.netCookieBuilder().name("appver").value("8.1.20").build());
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/api/v2/resource/comments/hug/listener";
    }
}
