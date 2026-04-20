package github.nooblong.btncm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 爱发电工具
 */
@Slf4j
@Component
public class AfdUtil {

    @Value("${afdToken}")
    private String token;

    @Value("${afdUserId}")
    private String userId;

    public JsonNode reqUser() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode allList = objectMapper.createArrayNode();
            int page = 1;
            int totalPage = 1; // 初始值，第一次请求后会更新
            int perPage = 20;

            while (page <= totalPage) {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("page", page);
                params.put("per_page", perPage);
                JsonNode resp = req(params, "https://afdian.com/api/open/query-sponsor");
                JsonNode data = resp.path("data");
                JsonNode list = data.path("list");
                if (list.isArray()) {
                    allList.addAll((ArrayNode) list);
                }
                totalPage = data.path("total_page").asInt();
                page++;
            }
            return allList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode reqOrder() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode allList = objectMapper.createArrayNode();
            int page = 1;
            int totalPage = 1; // 初始值，第一次请求后会更新
            int perPage = 50;

            while (page <= totalPage) {
                ObjectNode params = objectMapper.createObjectNode();
                params.put("page", page);
                params.put("per_page", perPage);
                JsonNode resp = req(params, "https://afdian.com/api/open/query-order");
                JsonNode data = resp.path("data");
                JsonNode list = data.path("list");
                if (list.isArray()) {
                    allList.addAll((ArrayNode) list);
                }
                totalPage = data.path("total_page").asInt();
                page++;
            }
            return allList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode req(JsonNode params, String url) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            long ts = System.currentTimeMillis() / 1000; // 当前秒级时间戳
            // 计算 sign
            String sign;
            log.info("md5参数: {}", token + "params" + objectMapper.writeValueAsString(params) + "ts" + ts + "user_id" + userId);
            sign = md5(token + "params" + objectMapper.writeValueAsString(params) + "ts" + ts + "user_id" + userId);
            // 构造请求 JSON
            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("user_id", userId);
            bodyMap.put("params", objectMapper.writeValueAsString(params));
            bodyMap.put("ts", ts);
            bodyMap.put("sign", sign);
            String jsonBody;
            jsonBody = objectMapper.writeValueAsString(bodyMap);
            // OkHttp POST
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                assert response.body() != null;
                String string = response.body().string();
                log.info("爱发电返回: " + string);
                return objectMapper.readTree(string);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 计算 MD5
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
