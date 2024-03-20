package github.nooblong.download.netmusic.module.weapi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.CryptoUtil;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 手机登录
 */
@Service
public class LoginCellphone extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("phone", (String) queryMap.get("phone"));
        node.put("countrycode", StrUtil.nullToDefault((String) queryMap.get("countrycode"), "86"));
        node.put("rememberLogin", true);
        if (queryMap.get("captcha") != null && StrUtil.isNotBlank(String.valueOf(queryMap.get("captcha")))) {
            node.put("captcha", String.valueOf(queryMap.get("captcha")));
        } else {
            String password = StrUtil.nullToDefault((String) queryMap.get("md5_password"), CryptoUtil.getMd5((String) queryMap.get("password")));
            node.put("password", password);
        }
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/login/cellphone";
    }
}
