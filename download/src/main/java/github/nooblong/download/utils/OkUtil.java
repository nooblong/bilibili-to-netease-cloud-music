package github.nooblong.download.utils;

import cn.hutool.core.util.XmlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.http.HttpHeaders;
import org.w3c.dom.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public class OkUtil {

    public static String anonymousToken = "1f5fa7b6a6a9f81a11886e5186fde7fb74afecc68539838b34fd4c1fc57b40cc4b7f5273fc3921d1f66807bb86d2c6cb1366ed0860abc362e189299846f5e4182c0190c476579d923324751bcc9aaf44c3061cd18d77b7a0";
    public static final String MOBILE_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
    public static final String PC_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
    public static final String WEAPI_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36 Edg/116.0.1938.69";
    public static final String LINUX_API_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36";

    public static Cookie.Builder netCookieBuilder() {
        return new Cookie.Builder().domain("music.163.com");
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
        Headers.Builder headerBuilder = new Headers.Builder();
        headerBuilder.set(HttpHeaders.REFERER, "https://music.163.com");
        headerBuilder.set(HttpHeaders.USER_AGENT, WEAPI_AGENT);
        header.forEach(headerBuilder::set);
        Headers headers = headerBuilder.build();
        log.info("请求网易WeApi Header: {}", headers.toMultimap());
        Request.Builder builder = new Request.Builder().url(url).headers(headers);
        Request request = builder.method(method.toUpperCase(), formBody).build();
        log.info("请求网易WeApi: {} body: {}", url, jsonNode);
        return request;
    }

    public static Request postApi(JsonNode jsonNode, String url, Map<String, String> header, String method) {
        Headers.Builder headerBuilder = new Headers.Builder();
        headerBuilder.set(HttpHeaders.REFERER, "https://music.163.com");
        headerBuilder.set(HttpHeaders.USER_AGENT, WEAPI_AGENT);
        header.forEach(headerBuilder::set);
        Headers headers = headerBuilder.build();
        FormBody.Builder formBuilder = new FormBody.Builder();
        jsonNode.fieldNames().forEachRemaining(s -> formBuilder.add(s, jsonNode.get(s).asText()));
        Request request = new Request.Builder().url(url).headers(headers)
                .method(method.toUpperCase(), formBuilder.build()).build();
        log.info("请求网易Api: {} body: {}", request, jsonNode);
        return request;
    }

    public static Request postEApi(JsonNode jsonNode, String url, String optionsUrl, Map<String, String> header, String method) {
        Headers.Builder headerBuilder = new Headers.Builder();
        headerBuilder.set(HttpHeaders.REFERER, "https://music.163.com");
        headerBuilder.set(HttpHeaders.USER_AGENT, WEAPI_AGENT);
        header.forEach(headerBuilder::set);
        Headers headers = headerBuilder.build();
        String eapiEncrypt = CryptoUtil.eapiEncrypt(url, jsonNode.toString());
        url = url.replaceAll("api", optionsUrl);
        RequestBody formBody = new FormBody.Builder().add("params", eapiEncrypt).build();
        Request request = new Request.Builder().url(url).headers(headers)
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
    public static JsonNode getJsonResponseNoLog(Request request, OkHttpClient client, Consumer<Response>... peeks) {
        try (Response response = client.newCall(request).execute()) {
            for (Consumer<Response> peek : peeks) {
                peek.accept(response);
            }
            ResponseBody body = response.body();
            assert body != null;
            String s = body.string();
            return new ObjectMapper().readTree(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public static Request getNoLog(String url) {
        return new Request.Builder().url(url).get().build();
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
