package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 默认搜索关键词
 */
@Service
public class SearchDefault extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://interface3.music.163.com/eapi/search/defaultkeyword/get";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/search/defaultkeyword/get";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {

    }
}
