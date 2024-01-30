package github.nooblong.download.utils;

import cn.hutool.core.util.XmlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class OkUtil {
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build();
    }


    public static String[] userAgentList = new String[]
            {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36 Edg/116.0.1938.69",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:80.0) Gecko/20100101 Firefox/80.0",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1",
                    "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36",
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36",
                    "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_2 like Mac OS X) AppleWebKit/603.2.4 (KHTML, like Gecko) Mobile/14F89;GameHelper",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/603.2.4 (KHTML, like Gecko) Version/10.1.1 Safari/603.2.4",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A300 Safari/602.1",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:46.0) Gecko/20100101 Firefox/46.0",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:46.0) Gecko/20100101 Firefox/46.0",
                    "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)",
                    "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0)",
                    "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)",
                    "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Win64; x64; Trident/6.0)",
                    "Mozilla/5.0 (Windows NT 6.3; Win64, x64; Trident/7.0; rv:11.0) like Gecko",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/13.10586",
                    "Mozilla/5.0 (iPad; CPU OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A300 Safari/602.1"
            };

    public static String getRandomUserAgent() {
        double index = Math.floor(Math.random() * userAgentList.length);
        return userAgentList[(int) index];
    }

    public static String getFixedUserAgent(int i) {
        return userAgentList[i];
    }

    public static Cookie.Builder netCookieBuilder() {
        return new Cookie.Builder().domain("music.163.com");
    }

    public static Headers getNetHeaders(Map<String, String> header) {
        Headers.Builder builder = new Headers.Builder();
        builder.set(HttpHeaders.ACCEPT, "*/*");
        builder.set(HttpHeaders.CONNECTION, "keep-alive");
        builder.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        builder.set(HttpHeaders.REFERER, "https://music.163.com");
        builder.set(HttpHeaders.USER_AGENT, getFixedUserAgent(0));
        header.forEach(builder::set);
        return builder.build();
    }

    public static Headers justAddHeaders(Map<String, String> header) {
        Headers.Builder builder = new Headers.Builder();
        header.forEach(builder::set);
        return builder.build();
    }

    public static Request postWeApi(JsonNode jsonNode, String url, Map<String, String> header, String method) {
        String encryptString = jsonNode.toString();
        String[] weapiEncrypt = CryptoUtil.weapiEncrypt(encryptString);
        FormBody formBody = new FormBody.Builder()
                .add("params", weapiEncrypt[0])
                .add("encSecKey", weapiEncrypt[1])
                .build();
        Headers netHeaders = getNetHeaders(header);
        log.info("请求网易WeApi Header: {}", netHeaders.toMultimap());
        Request.Builder builder = new Request.Builder().url(url).headers(netHeaders);
        Request request = builder.method(method.toUpperCase(), formBody).build();
        log.info("请求网易WeApi: {} body: {}", url, jsonNode);
        return request;
    }

    public static Request postApi(JsonNode jsonNode, String url, Map<String, String> header, String method) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        jsonNode.fieldNames().forEachRemaining(s -> formBuilder.add(s, jsonNode.get(s).asText()));
        Request request = new Request.Builder().url(url).headers(getNetHeaders(header))
                .method(method.toUpperCase(), formBuilder.build()).build();
        log.info("请求网易Api: {} body: {}", request, jsonNode);
        return request;
    }

    public static Request postEApi(JsonNode jsonNode, String url, String optionsUrl, Map<String, String> header, String method) {
        String eapiEncrypt = CryptoUtil.eapiEncrypt(url, jsonNode.toString());
        url = url.replaceAll("api", optionsUrl);
        RequestBody formBody = new FormBody.Builder().add("params", eapiEncrypt).build();
        Request request = new Request.Builder().url(url).headers(getNetHeaders(header))
                .method(method.toUpperCase(), formBody).build();
        log.info("请求网易EApi: {} body: {}", request, jsonNode);
        return request;
    }

    public static Request uploadWeApi(InputStream inputStream, String url, Map<String, String> header, String method,
                                      String mediaType) throws FileNotFoundException {
        RequestBody requestBody = RequestBodyUtil.create(MediaType.parse(mediaType), inputStream);
        Request request = new Request.Builder()
                .headers(justAddHeaders(header))
                .url(url).method(method.toUpperCase(), requestBody).build();
        log.info("上传网易云: {}", request);
        return request;
    }

    public static Request uploadImgWeapi(InputStream inputStream, String xNosToken, String objectKey) {
        RequestBody requestBody = RequestBodyUtil.create(MediaType.parse("image/jpeg"), inputStream);
        Request request = new Request.Builder().url("https://nosup-hz1.127.net/yyimgs/" +
                        objectKey + "?offset=0&complete=true&version=1.0").post(requestBody)
                .header("Content-Type", "image/jpeg")
                .addHeader("x-nos-token", xNosToken)
                .build();
        log.info("上传网易云图片: {}", request);
        return request;
    }

    @SafeVarargs
    public static JsonNode getJsonResponse(Request request, OkHttpClient client, Consumer<Response>... peeks) {
        try (Response response = client.newCall(request).execute()) {
            for (Consumer<Response> peek : peeks) {
                peek.accept(response);
            }
            ResponseBody body = response.body();
            assert body != null;
            String s = body.string();
            log.info("response(code:{}): {}", response.code(), s.substring(0, Math.min(s.length(), 500)));
            return new ObjectMapper().readTree(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public static JsonNode getJsonResponseWithHeader(Request request, OkHttpClient client, Consumer<Response>... peeks) {
        try (Response response = client.newCall(request).execute()) {
            for (Consumer<Response> peek : peeks) {
                peek.accept(response);
            }
            ResponseBody body = response.body();
            assert body != null;
            String s = body.string();
            Map<String, List<String>> headerMap = response.headers().toMultimap();
            JsonNode readTree = new ObjectMapper().readTree(s);
            ObjectNode jsonNode;
            if (readTree.isObject()) {
                jsonNode = (ObjectNode) readTree;
            } else {
                jsonNode = new ObjectMapper().createObjectNode();
            }
            headerMap.forEach((key, value) -> jsonNode.put(key, value.get(0)));
            log.info("response(code:{}): {}", response.code(), s.substring(0, Math.min(s.length(), 500)));
            return jsonNode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public static Document getXmlResponse(Request request, OkHttpClient client, Consumer<Response>... peeks) {
        try (Response response = client.newCall(request).execute()) {
            for (Consumer<Response> peek : peeks) {
                peek.accept(response);
            }
            ResponseBody body = response.body();
            assert body != null;
            String s = body.string();
            log.info("response(code:{}): {}", response.code(), s);
            String cleaned = XmlUtil.cleanInvalid(s);
            return XmlUtil.parseXml(cleaned);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStringResponse(Request request, OkHttpClient client) {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            assert body != null;
            String s = body.string();
            log.info("response(code:{}): {}", response.code(), s.substring(0, Math.min(s.length(), 500)));
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Request get(String url) {
        Request request = new Request.Builder().url(url).get().build();
        log.info("GET: {}", request);
        return request;
    }

    public static Request get(HttpUrl url) {
        Request request = new Request.Builder().url(url).get().build();
        log.info("GET: {}", request);
        return request;
    }

    public static Request get(String url, Map<String, String> params) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        params.forEach(builder::addQueryParameter);
        Request request = new Request.Builder().url(builder.build()).get().build();
        log.info("GET: {}", request);
        return request;
    }

    public static Request get(String url, Map<String, String> params, Map<String, String> headers) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        params.forEach(builder::addQueryParameter);
        Request request = new Request.Builder().url(builder.build()).headers(justAddHeaders(headers)).get().build();
        log.info("GET: {}", request);
        return request;
    }

    public static Request post(JsonNode body, String url) {
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), body.toString());
        Request request = new Request.Builder().url(url)
                .method("POST", requestBody)
                .build();
        log.info("POST: {}", request);
        assert request.body() != null;
        return request;
    }

    public static Request post(JsonNode body, HttpUrl url) {
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), body.toString());
        Request request = new Request.Builder().url(url)
                .method("POST", requestBody)
                .build();
        log.info("POST: {}", request);
        assert request.body() != null;
        return request;
    }

    public static Request post(JsonNode body, String url, Map<String, String> header) {
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), body.toString());
        Request request = new Request.Builder().url(url)
                .method("POST", requestBody)
                .headers(justAddHeaders(header))
                .build();
        log.info("POST: {}", request);
        assert request.body() != null;
        return request;
    }

    public static Request post(String body, String contentType, String url, Map<String, String> header, String method) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(contentType), body);
        Request request = new Request.Builder().url(url)
                .method(method.toUpperCase(), requestBody)
                .headers(justAddHeaders(header))
                .build();
        log.info("POST: {}", request);
        assert request.body() != null;
        return request;
    }


}
