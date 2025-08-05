package github.nooblong.download.netmusic;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.util.JwtUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NetMusicForwardInterceptor implements HandlerInterceptor {

    @Autowired
    NetMusicClient bs;

    @Override
    public boolean preHandle(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        Map<String, Object> queryMap = new ConcurrentHashMap<>();
        if (StrUtil.isNotEmpty(request.getQueryString())) {
            String[] queryArray = request.getQueryString().split("&");
            for (String query : queryArray) {
                if (query.contains("=")) {
                    String[] split = query.split("=");
                    queryMap.put(split[0], split[1]);
                }
            }
        }

        String key = request.getRequestURI()
                .replaceAll("direct", "")
                .replaceAll("/", "");

        JsonNode musicData;
        PrintWriter writer = null;

        if (request.getMethod().equals("OPTIONS")) {
            try {
                response.setContentType("text/html;charset=utf-8");
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "Content-Type,Access-Token");
                writer = response.getWriter();
                writer.print("");
            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                assert writer != null;
                writer.close();
            }
            return false;
        }
        SysUser sysUser = JwtUtil.verifierFromContext();
        musicData = bs.getMusicDataByUserId(queryMap, key, sysUser.getId());
        log.info("response: {}", musicData.toString());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,Access-Token");
        try {
            writer = response.getWriter();
            writer.print(musicData);
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            assert writer != null;
            writer.close();
        }
        return false;
    }
}
