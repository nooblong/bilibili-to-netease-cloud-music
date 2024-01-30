package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
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

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        this.token = (String) queryMap.get("token");
        this.objectKey = ((String) queryMap.get("objectKey")).replace("/", "%2F");
        this.uploadId = (String) queryMap.get("uploadId");
        this.dataInputStream = (InputStream) queryMap.get("dataInputStream");
        Assert.notNull(queryMap.get("token"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("objectKey"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("uploadId"), "AudioUploadSecond缺少参数");
        Assert.notNull(queryMap.get("dataInputStream"), "AudioUploadSecond缺少参数");
    }

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("x-nos-token", token);
        headerMap.put("X-Nos-Meta-Content-Type", "audio/mpeg");
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] buffer = new byte[5242880];
        ArrayNode responseArray = objectMapper.createArrayNode();
        try (ReadableByteChannel sourceChannel = Channels.newChannel(dataInputStream)) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int partNum = 1;
            while (sourceChannel.read(byteBuffer) > 0) {
                byteBuffer.flip();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, byteBuffer.limit());
                try {
                    JsonNode responseWithHeader = OkUtil.getJsonResponseWithHeader(OkUtil.uploadWeApi(byteArrayInputStream,
                            "https://ymusic.nos-hz.163yun.com/" + objectKey
                                    + "?partNumber=" + partNum
                                    + "&uploadId=" + uploadId,
                            headerMap, getMethod(), "audio/mpeg"), client);
                    partNum++;
                    responseArray.add(responseWithHeader);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assert !responseArray.isEmpty();
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
