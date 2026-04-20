package github.nooblong.btncm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.entity.SysUser;
import okhttp3.Cookie;

import java.util.List;
import java.util.Map;

public interface IUserService extends IService<SysUser> {
    /**
     * 获取cookie
     */
    Map<String, String> getBilibiliCookieMap(Long id);

    /**
     * 获取cookie
     */
    Map<String, String> getNeteaseCookieMap(Long id);

    /**
     * 获取cookie
     */
    List<Cookie> getNeteaseOkhttpCookie(Long id);

    /**
     * 更新cookie
     */
    void updateNeteaseCookieByOkhttpCookie(Long id, List<Cookie> cookieList);

    /**
     * 更新cookie
     */
    void updateNeteaseCookieByCookieMap(Long id, Map<String, String> cookieMap);

    /**
     * 更新cookie
     */
    void updateBilibiliCookieByCookieMap(Long id, Map<String, String> cookieMap);

    /**
     * 更新cookie
     */
    void updateBilibiliCookieByOkhttpCookie(Long id, List<Cookie> cookieList);

    /**
     * 统计信息
     */
    Integer visitTimes();

    /**
     * 统计信息
     */
    Integer visitToday();

    /**
     * 统计信息
     */
    Integer visitTodayTimes();
}
