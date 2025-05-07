package github.nooblong.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.mapper.UserMapper;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.CommonUtil;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements IUserService {

    @Nonnull
    @Override
    public Map<String, String> getBilibiliCookieMap(Long id) {
        SysUser byId = getById(id);
        String biliCookies = byId.getBiliCookies();
        return CommonUtil.convertJsonToMap(biliCookies);
    }

    @Nonnull
    @Override
    public Map<String, String> getNeteaseCookieMap(Long id) {
        SysUser byId = getById(id);
        String netCookies = byId.getNetCookies();
        return CommonUtil.convertJsonToMap(netCookies);
    }



    @Nonnull
    @Override
    public List<Cookie> getNeteaseOkhttpCookie(Long id) {
        Map<String, String> neteaseCookieMap = getNeteaseCookieMap(id);
        List<Cookie> cookies = new ArrayList<>();
        neteaseCookieMap.forEach((k, v) -> {
            cookies.add(new Cookie.Builder().name(k)
                    .value(v)
                    .domain("music.163.com")
                    .build());
        });
        return cookies;
    }

    @Override
    public void updateNeteaseCookieByOkhttpCookie(Long id, List<Cookie> cookieList) {
        SysUser byId = getById(id);
        if (cookieList == null || cookieList.isEmpty()) {
            return;
        }
        ObjectNode jsonNodes = CommonUtil.cookieListToObjectNode(cookieList);
        byId.setNetCookies(jsonNodes.toString());
        updateById(byId);
    }

    @Override
    public void updateNeteaseCookieByCookieMap(Long id, Map<String, String> cookieMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String cookie = objectMapper.writeValueAsString(cookieMap);
            SysUser byId = getById(id);
            byId.setNetCookies(cookie);
            updateById(byId);
        } catch (JsonProcessingException e) {
            log.error("更新网易cookie失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateBilibiliCookieByCookieMap(Long id, Map<String, String> cookieMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String cookie = objectMapper.writeValueAsString(cookieMap);
            SysUser byId = getById(id);
            byId.setBiliCookies(cookie);
            updateById(byId);
        } catch (JsonProcessingException e) {
            log.error("更新b站cookie失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateBilibiliCookieByOkhttpCookie(Long id, List<Cookie> cookieList) {
        SysUser byId = getById(id);
        if (cookieList == null || cookieList.isEmpty()) {
            return;
        }
        ObjectNode jsonNodes = CommonUtil.cookieListToObjectNode(cookieList);
        byId.setBiliCookies(jsonNodes.toString());
        updateById(byId);
    }

    @Override
    public Integer visitTimes() {
        return baseMapper.visitTimes();
    }

    @Override
    public Integer visitToday() {
        return baseMapper.visitToday();
    }

    @Override
    public Integer visitTodayTimes() {
        return baseMapper.visitTodayTimes();
    }
}
