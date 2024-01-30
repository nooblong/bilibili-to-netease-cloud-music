package github.nooblong.download.netmusic.module.notype;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleNoTypeModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 首页轮播图
 */
@Service
public class Banner extends SimpleNoTypeModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        if (StrUtil.isEmpty((String) queryMap.get("type"))) {
            node.put("clientType", "pc");
        } else if (StrUtil.equals("0", (String) queryMap.get("type"))) {
            node.put("clientType", "pc");
        } else if (StrUtil.equals("1", (String) queryMap.get("type"))) {
            node.put("clientType", "android");
        } else if (StrUtil.equals("2", (String) queryMap.get("type"))) {
            node.put("clientType", "iphone");
        } else if (StrUtil.equals("3", (String) queryMap.get("type"))) {
            node.put("clientType", "ipad");
        } else {
            node.put("clientType", "pc");
        }
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/api/v2/banner/get";
    }
}
