package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 检测手机号码是否已注册
 */
@Service
public class CellphoneExistenceCheck extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://music.163.com/eapi/cellphone/existence/check";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/cellphone/existence/check";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("cellphone", (String) queryMap.get("phone"));
        node.put("countrycode", (String) queryMap.get("countrycode"));
    }
}
