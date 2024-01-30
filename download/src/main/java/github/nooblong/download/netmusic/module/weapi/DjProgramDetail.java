package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author wjning
 * @date 2021/6/8 9:24
 * @description 电台节目详情
 */
@Service
public class DjProgramDetail extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", Integer.parseInt((String) queryMap.get("id")));
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/api/dj/program/detail";
    }
}
