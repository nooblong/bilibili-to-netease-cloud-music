package github.nooblong.download.netmusic.module.eapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.TypeConfig;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 首页-发现 dragon ball
 * 这个接口为移动端接口，首页-发现页（每日推荐、歌单、排行榜 那些入口）
 * !需要登录或者匿名登录，非登录返回 []
 */
@Service
public class HomepageDragonBall extends SimpleEApiModule {

    @Override
    public String getUrl() {
        return "https://music.163.com/eapi/homepage/dragon/ball/static";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/homepage/dragon/ball/static";
    }

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("pc").build());
        cookieList.add(OkUtil.netCookieBuilder().name("appver").value("8.1.20").build());
        boolean anno = true;
        for (Cookie cookie : cookieList) {
            if (cookie.name().equals("MUSIC_U") || cookie.value().equals("MUSIC_U")) {
                anno = false;
                break;
            }
        }
        if (anno) {
            cookieList.add(OkUtil.netCookieBuilder().name("MUSIC_A").value(TypeConfig.anonymous_token).build());
        }
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {

    }
}
