package github.nooblong.btncm.netmusic.api.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.btncm.netmusic.SimpleWeApiModule;
import github.nooblong.btncm.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LoginRefresh extends SimpleWeApiModule {

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("ios").build());
        cookieList.add(OkUtil.netCookieBuilder().name("appver").value("8.20.21").build());
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
    }

    @Override
    public String getUrl() {
        return "https://music.163.com/weapi/login/token/refresh";
    }
}
