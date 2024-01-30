package github.nooblong.download.netmusic;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.common.model.Result;
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
        try {
            musicData = bs.getMusicDataByContext(queryMap, key);
        } catch (ValidateException | IllegalArgumentException e) {
            log.error(e.getMessage());
            musicData = new ObjectMapper().valueToTree(Result.fail(""));
        }
        log.info("response: {}", musicData.toString());
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=utf-8");
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
