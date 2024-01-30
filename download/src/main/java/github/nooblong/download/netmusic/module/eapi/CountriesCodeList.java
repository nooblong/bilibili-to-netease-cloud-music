package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 国家编码列表
 */
@Service
public class CountriesCodeList extends SimpleEApiModule {


    @Override
    public String getUrl() {
        return "https://interface3.music.163.com/eapi/lbs/countries/v1";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/lbs/countries/v1";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {

    }
}
