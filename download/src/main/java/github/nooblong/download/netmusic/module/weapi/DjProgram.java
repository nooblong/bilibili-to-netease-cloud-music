package github.nooblong.download.netmusic.module.weapi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author wjning
 * @date 2021/6/8 9:24
 * @description 电台节目列表
 */
@Service
public class DjProgram extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("limit", StrUtil.nullToDefault((String) queryMap.get("limit"), "30"));
        node.put("offset", StrUtil.nullToDefault((String) queryMap.get("offset"), "0"));
        node.put("radioId", (String) queryMap.get("rid"));
        node.put("asc", (String) queryMap.get("asc"));
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/dj/program/byradio";
    }
}
