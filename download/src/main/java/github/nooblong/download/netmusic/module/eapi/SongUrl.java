package github.nooblong.download.netmusic.module.eapi;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleEApiModule;
import github.nooblong.download.utils.CryptoUtil;
import github.nooblong.download.utils.OkUtil;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 歌曲链接
 */
@Service
public class SongUrl extends SimpleEApiModule {

    @Override
    public void genCookie(List<Cookie> cookieList) {
        cookieList.add(OkUtil.netCookieBuilder().name("os").value("pc").build());
        boolean hasU = false;
        for (Cookie cookie : cookieList) {
            if (cookie.name().equals("MUSIC_U") || cookie.value().equals("MUSIC_U")) {
                hasU = true;
                break;
            }
        }
        if (!hasU) {
            cookieList.add(OkUtil.netCookieBuilder().name("_ntes_nuid").value(CryptoUtil.strToHex(CryptoUtil.createSecretKey())).build());
        }
    }

    @Override
    public String getUrl() {
        return "https://interface3.music.163.com/eapi/song/enhance/player/url";
    }

    @Override
    public String getOptionsUrl() {
        return "/api/song/enhance/player/url";
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("br", StrUtil.nullToDefault((String) queryMap.get("br"), "999000"));
        node.put("id", (String) queryMap.get("id"));
    }
}
