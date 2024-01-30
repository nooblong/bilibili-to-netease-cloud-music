package github.nooblong.download.netmusic.module.eapi;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.TypeConfig;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 评论
 */
@Service
public class CommentNew extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://music.163.com/api/v2/resource/comments";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/v2/resource/comments";
    }

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("pc").build());
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        String type = TypeConfig.resourceTypeMap.get((String) queryMap.get("type"));
        node.put("threadId", type + queryMap.get("id"));
        node.put("pageSize", StrUtil.nullToDefault((String) queryMap.get("pageSize"), "20"));
        node.put("showInner", StrUtil.nullToDefault((String) queryMap.get("showInner"), "true"));
        node.put("pageNo", StrUtil.nullToDefault((String) queryMap.get("pageNo"), "1"));
        String cursor;
        if (StrUtil.equals((String) queryMap.get("sortType"), "3")) {
            cursor = StrUtil.nullToDefault((String) queryMap.get("cursor"), "0");
        } else {
            cursor = NumberUtil.mul(StrUtil.nullToDefault((String) queryMap.get("pageNo"), "1"), StrUtil.nullToDefault((String) queryMap.get("pageSize"), "20")).toString();
        }
        node.put("cursor", cursor);
        node.put("sortType", (String) queryMap.get("sortType"));
    }
}
