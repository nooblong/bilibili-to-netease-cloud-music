package github.nooblong.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import okhttp3.Cookie;

import java.util.List;
import java.util.Map;

public interface IUserService extends IService<SysUser> {
    Map<String, String> getBilibiliCookieMap(Long id);

    Map<String, String> getNeteaseCookieMap(Long id);

    List<Cookie> getNeteaseOkhttpCookie(Long id);

    void updateNeteaseCookieByOkhttpCookie(Long id, List<Cookie> cookieList);

    void updateBilibiliCookieByCookieMap(Long id, Map<String, String> cookieMap);

    void updateBilibiliCookieByOkhttpCookie(Long id, List<Cookie> cookieList);

    ObjectNode cookieListToObjectNode(List<Cookie> cookieList);

    Integer sumVisitTime();
}
