package github.nooblong.download.netmusic.module.weapi;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import github.nooblong.download.service.UploadDetailService;
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

    public void logNow(Long uploadDetailId, String content) {
        UploadDetail uploadDetail = Db.getById(uploadDetailId, UploadDetail.class);
        uploadDetail.setLog(CommonUtil.processString(uploadDetail.getLog()) +
                DateUtil.now() + " " + content + "\n");
        Db.updateById(uploadDetail);
    }

    @Override
    public JsonNode execute(JsonNode paramNode, Map<String, String> headerMap, OkHttpClient client) {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] buffer = new byte[5242880];
        logNow(uploadDetailId, ">>> 开始上传");
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
                    logNow(uploadDetailId, ">>> 已上传5Mb");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            logNow(uploadDetailId, ">>> 上传结束");
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
