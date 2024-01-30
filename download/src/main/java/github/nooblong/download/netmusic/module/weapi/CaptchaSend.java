package github.nooblong.download.netmusic.module.weapi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CaptchaSend extends SimpleWeApiModule {

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.removeAll(cookieList);
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("ios").build());
        cookieList.add(OkUtil.netCookieBuilder().name("appver").value("8.20.21").build());
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("ctcode", StrUtil.nullToDefault((String) queryMap.get("countrycode"), "86"));
        node.put("cellphone", (String) queryMap.get("phone"));
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/sms/captcha/sent";
    }
}
