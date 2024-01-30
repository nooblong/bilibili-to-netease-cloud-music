package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.BaseTest;
import github.nooblong.download.netmusic.NetMusicClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class UploadTest extends BaseTest {

    @Autowired
    NetMusicClient netMusicClient;

    @Test
    public void testUpload() {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("ext", "mp3");
        queryMap.put("fileName", "testmcao");
        JsonNode audiouploadalloc = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadalloc", 1L);
        System.out.println(audiouploadalloc.toPrettyString());

        String docId = audiouploadalloc.get("result").get("docId").asText();
        String objectKey = audiouploadalloc.get("result").get("objectKey").asText();
        String token = audiouploadalloc.get("result").get("token").asText();
        queryMap.put("docId", docId);
        queryMap.put("objectKey", objectKey);
        queryMap.put("token", token);

        JsonNode audiouploadfirst = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadfirst", 1L);
        String uploadId = audiouploadfirst.get("uploadId").asText();
        System.out.println(uploadId);

        queryMap.put("uploadId", uploadId);
        Path path2 = Paths.get("/Users/lyl/Downloads/mp4-tmp/卫兰-大哥.mp3");
        try {
            queryMap.put("dataInputStream", new FileInputStream(path2.toFile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonNode audiouploadsecond = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadsecond", 1L);
        String etag = audiouploadsecond.get("etag").asText();
        System.out.println(etag);

        queryMap.put("name", "测试1");
        queryMap.put("voiceListId", "994819294");
        queryMap.put("coverImgId", "109951168896496224");
        queryMap.put("categoryId", "3");
        queryMap.put("secondCategoryId", "469050");
        queryMap.put("privacy", "false");
        JsonNode audioprecheck = netMusicClient.getMusicDataByUserId(queryMap, "audioprecheck", 1L);

        JsonNode audioupload = netMusicClient.getMusicDataByUserId(queryMap, "audiosubmit", 1L);

        System.out.println(audioupload.toPrettyString());

    }

}
