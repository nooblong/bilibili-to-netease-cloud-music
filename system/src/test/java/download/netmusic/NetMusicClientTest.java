package download.netmusic;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import download.BaseTest;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
class NetMusicClientTest extends BaseTest {

    @Test
    public void testTrans() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("programId", "2532916453");
        hashMap.put("radioId", "996517258");
        hashMap.put("position", "999");
//        hashMap.put("useCookie", "1");
        JsonNode audioprogramtrans = netMusicClient.getMusicDataByUserId(hashMap, "audioprogramtrans", 1L);
        System.out.println(audioprogramtrans.toPrettyString());
    }

    @Test
    void getUserVoicelist() {
        JsonNode userVoiceList = netMusicClient.getUserVoiceList(53L);
        System.out.println(userVoiceList.toPrettyString());
    }

    @Test
    public void testDeleteMusic() throws InterruptedException {
        String voiceListId = "996002302";
        for (int i = 0; i < 185; i++) {
            JsonNode voiceList = netMusicClient.getVoiceList(voiceListId, 1L, i * 20L);
            ArrayNode list = (ArrayNode) voiceList.get("list");
            System.out.println(list.size());
            for (JsonNode jsonNode : list) {
//                if (!jsonNode.get("displayStatus").asText().equals("ONLINE")) {
                if (jsonNode.get("voiceName").asText().contains("水友")) {
                    log.info("删除{}, {}", jsonNode.get("voiceName").asText(), jsonNode.get("displayStatus").asText());
                    String voiceId = jsonNode.get("voiceId").asText();
//                    Map<String, Object> param = new HashMap<>();
//                    param.put("programId", voiceId);
//                    param.put("radioId", voiceListId);
//                    param.put("position", "9999");
//                    JsonNode audioprogramtrans = netMusicClient.getMusicDataWithAdmin(param, "audioprogramtrans");

                    Map<String, Object> param = new HashMap<>();
                    param.put("voiceIds", voiceId);
                    param.put("voiceListId", Long.valueOf(voiceListId));
                    JsonNode voiceremove = netMusicClient.getMusicDataByUserId(param, "voiceremove", 1L);
                    Thread.sleep(1000);
                }
            }
        }

    }

    @Test
    public void testTransYULYLtoBottom() throws Exception {
        String voiceListId = "996002302";
//        String voiceListId = "994819294";//测试
        List<SortSong> yulylList = new ArrayList<>();
        List<SortSong> hasTimeList = new ArrayList<>();
        List<SortSong> unknownList = new ArrayList<>();
        List<SortSong> allList = new ArrayList<>();
        for (int i = 0; i < 172; i++) {
            JsonNode voiceList = netMusicClient.getVoiceList(voiceListId, 1L, i * 20L);
            ArrayNode list = (ArrayNode) voiceList.get("list");
            for (JsonNode jsonNode : list) {
                String voiceId = jsonNode.get("voiceId").asText();
                String voiceName = jsonNode.get("voiceName").asText();

                if (voiceName.equals("yulyl的声音")) {
                    log.info("yulyl: {}", voiceName);
                    yulylList.add(new SortSong().setVoiceId(voiceId).setVoiceName(voiceName));
                    continue;
                }

                String datePattern = "\\（(.*?)\\）";
                String s1 = ReUtil.extractMulti(datePattern, voiceName, "$1");

                if (StrUtil.isBlank(s1)) {
                    log.info("没有括号: {}", voiceName);
                    unknownList.add(new SortSong().setVoiceName(voiceName).setVoiceId(voiceId));
                    continue;
                }

                try {
                    DateTime parse = DateUtil.parse(s1, "yyyy.MM.dd");
                    String format = DateUtil.format(parse, "yyyy-MM-dd");
                    log.info("拥有时间: {}, {}", voiceName, format);
                    hasTimeList.add(new SortSong().setVoiceName(voiceName).setVoiceId(voiceId).setDate(parse));
                } catch (Exception e) {
                    log.error("时间解析错误: {}, {}", voiceName, e.getMessage());
                    unknownList.add(new SortSong().setVoiceId(voiceId).setVoiceName(voiceName));
                }
            }
            Thread.sleep(500);
        }
        log.info("start!");
        Collections.sort(hasTimeList);
        allList.addAll(hasTimeList);
        allList.addAll(unknownList);
        allList.addAll(yulylList);
        log.info("allList size: {}", allList.size());
        for (int i = 1; i < allList.size() + 1; i++) {
            boolean ok = false;
            do {
                SortSong sortSong = allList.get(i);
                log.info("处理 {} : {}", i, sortSong.getVoiceName());
                Map<String, Object> param = new HashMap<>();
                param.put("programId", sortSong.voiceId);
                param.put("radioId", voiceListId);
                param.put("position", String.valueOf(i));
                JsonNode audioprogramtrans = netMusicClient.getMusicDataByUserId(param, "audioprogramtrans", 1L);
                if (audioprogramtrans.get("code").asInt() == 200) {
                    ok = true;
                    // 获取一遍第一页
                } else {
                    log.error("失败重试: {}", sortSong.getVoiceName());
                }
            } while (!ok);
        }


        System.out.println("ok");

    }

    @Data
    @Accessors(chain = true)
    private static class SortSong implements Comparable<SortSong> {
        private String voiceName;
        private String voiceId;
        private Date date;

        @Override
        public int compareTo(SortSong o) {
            return o.getDate().compareTo(date);
        }
    }

    @Test
    public void testTransByRegex() throws Exception {
        String voiceListId = "996002302";
//        String voiceListId = "994819294";//测试
        List<SortSong> yulylList = new ArrayList<>();
        List<SortSong> hasTimeList = new ArrayList<>();
        List<SortSong> unknownList = new ArrayList<>();
        List<SortSong> allList = new ArrayList<>();
        for (int i = 0; i < 172; i++) {
            JsonNode voiceList = netMusicClient.getVoiceList(voiceListId, 1L, i * 20L);
            ArrayNode list = (ArrayNode) voiceList.get("list");
            Assert.isTrue(!list.isEmpty(), "空！");
            for (JsonNode jsonNode : list) {
                String voiceId = jsonNode.get("voiceId").asText();
                String voiceName = jsonNode.get("voiceName").asText();

                if (voiceName.equals("yulyl的声音")) {
                    log.info("yulyl: {}", voiceName);
                    yulylList.add(new SortSong().setVoiceId(voiceId).setVoiceName(voiceName));
                    continue;
                }

                String datePattern = "\\（(.*?)\\）";
                String s1 = ReUtil.extractMulti(datePattern, voiceName, "$1");

                if (StrUtil.isBlank(s1)) {
                    log.info("没有括号: {}", voiceName);
                    unknownList.add(new SortSong().setVoiceName(voiceName).setVoiceId(voiceId));
                    continue;
                }

                try {
                    DateTime parse = DateUtil.parse(s1, "yyyy.MM.dd");
                    String format = DateUtil.format(parse, "yyyy-MM-dd");
                    log.info("拥有时间: {}, {}", voiceName, format);
                    hasTimeList.add(new SortSong().setVoiceName(voiceName).setVoiceId(voiceId).setDate(parse));
                } catch (Exception e) {
                    log.error("时间解析错误: {}, {}", voiceName, e.getMessage());
                    unknownList.add(new SortSong().setVoiceId(voiceId).setVoiceName(voiceName));
                }
            }
        }
        log.info("start!");
        Collections.sort(hasTimeList);
        allList.addAll(hasTimeList);
        allList.addAll(unknownList);
        allList.addAll(yulylList);
//        Collections.reverse(allList);
        log.info("allList size: {}", allList.size());
        for (int i = 0; i < allList.size(); i++) {
            boolean ok = false;
            do {
                SortSong sortSong = allList.get(i);
                log.info("处理 {} : {}", i, sortSong.getVoiceName());
                Map<String, Object> param = new HashMap<>();
                param.put("programId", sortSong.voiceId);
                param.put("radioId", voiceListId);
                param.put("position", "1");
                JsonNode audioprogramtrans = netMusicClient.getMusicDataByUserId(param, "audioprogramtrans", 1L);
                int code = audioprogramtrans.get("code").asInt();
//                if (audioprogramtrans.get("code").asInt() == 200) {
                // 获取一遍第一页
                JsonNode voiceList = netMusicClient.getVoiceList(voiceListId, 1L, 0L);
                // 确保移动到第一
                ArrayNode list = (ArrayNode) voiceList.get("list");
                JsonNode first = list.get(0);
                for (int j = 0; j < 2; j++) {
                    if (first.get("voiceId").asText().equals(sortSong.getVoiceId())) {
                        ok = true;
                        break;
                    }
                    log.error("失败重试: {}", sortSong.getVoiceName());
                    if (code == 200) {
                        Thread.sleep(1000);
                    } else {
                        Thread.sleep(500);
                    }
                }
            } while (!ok);
        }
        System.out.println("ok");

    }

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