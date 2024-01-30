package github.nooblong.download.netmusic.module.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author wjning
 * @date 2021/6/7 16:37
 * @description 所有榜单介绍
 */
@Service
public class Toplist extends SimpleApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {

    }

    @Override
    public String getUrl() {
        return "https://music.163.com/api/toplist";
    }
}
