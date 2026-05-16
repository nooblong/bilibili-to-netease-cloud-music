package github.nooblong.btncm.netmusic.api.weapi;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.netmusic.SimpleWeApiModule;
import github.nooblong.btncm.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

@Slf4j
@Service
@Scope("prototype")
public class AudioUploadSecond extends SimpleWeApiModule {
    private String token;
    private String uploadId;
    private String objectKey;
    private InputStream dataInputStream;
    private Long uploadDetailId;

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        this.token = (String) queryMap.get("token");
        this.objectKey = ((String) queryMap.get("objectKey")).replace("/", "%2F");
        this.uploadId = (String) queryMap.get("uploadId");
        this.dataInputStream = (InputStream) queryMap.get("dataInputStream");
        this.uploadDetailId = (Long) queryMap.get("uploadDetailId");
        Assert.notNull(queryMap.get("token"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("objectKey"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("uploadId"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("dataInputStream"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("uploadDetailId"), "AudioUploadSecond缺少参数uploadDetailId");
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("x-nos-token", token);
        headerMap.put("X-Nos-Meta-Content-Type", "audio/mpeg");
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        final int CHUNK_SIZE = 1024 * 1024; // 1MB
        byte[] buffer = new byte[CHUNK_SIZE];
        log.info(">>> 开始上传");
        ArrayNode responseArray = objectMapper.createArrayNode();

        try (ReadableByteChannel sourceChannel = Channels.newChannel(dataInputStream)) {
            int partNum = 1;
            int bytesRead;

            while ((bytesRead = sourceChannel.read(ByteBuffer.wrap(buffer))) > 0) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);

                boolean success = false;
                int retry = 0;
                final int maxRetry = 3;

                while (!success) {
                    try {
                        JsonNode responseWithHeader = OkUtil.getJsonResponseWithHeader(
                        OkUtil.uploadWeApi(
                                        bytesRead,
                                        byteArrayInputStream,
                                        "https://ymusic.nos-hz.163yun.com/" + objectKey
                                                + "?partNumber=" + partNum
                                                + "&uploadId=" + uploadId,
                                        headerMap, getMethod(), "audio/mpeg"
                                ),
                                client
                        );
                        responseArray.add(responseWithHeader);
                        log.info(">>> 上传分片 #{} 成功，{} b", partNum, bytesRead);
                        success = true;
                    } catch (Exception e) {
                        retry++;
                        if (retry < maxRetry) {
                            log.warn("上传分片 #{} 失败，重试中({}/{})...", partNum, retry, maxRetry);
                            byteArrayInputStream.reset(); // 重要：重试时重置流
                        } else {
                            log.error("上传分片 #{} 多次失败，终止上传。", partNum);
                            throw new RuntimeException("上传失败", e);
                        }
                    }
                }
                partNum++;
            }

            log.info(">>> 上传结束");

        } catch (IOException e) {
            throw new RuntimeException("读取上传数据失败", e);
        }

        if (responseArray.isEmpty()) {
            throw new RuntimeException("上传结果为空");
        }

        return responseArray;
    }

    @Override
    public String getMethod() {
        return "put";
    }

    @Override
    public String getUrl() {
        return "";
    }
}
