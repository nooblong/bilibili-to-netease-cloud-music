package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AudioUpdate extends SimpleWeApiModule {

    @Override
    public void genHeader(Map<String, String> headerMap) {
        headerMap.put("Host", "interface.music.163.com");
        headerMap.put("X-Real-IP:", "::1");
        headerMap.put("X-Forwarded-For:", "::1");
    }

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("name", queryMap.get("name") != null ? (String) queryMap.get("name") : "空白的名称");
        objectNode.put("autoPublish", false);
        objectNode.put("autoPublishText", "");
        objectNode.put("description", queryMap.get("description") != null ? (String) queryMap.get("description") :
                "upload by github.com/nooblong/bilibili-to-netease-cloud-music");
        objectNode.put("voiceListId", (String) queryMap.get("voiceListId"));
        objectNode.put("coverImgId", (String) queryMap.get("coverImgId"));
        objectNode.put("dfsId", (String) queryMap.get("docId"));
        objectNode.put("categoryId", (String) queryMap.get("categoryId"));
        objectNode.put("secondCategoryId", (String) queryMap.get("secondCategoryId"));
        objectNode.set("composedSongs", objectMapper.createArrayNode()
//                .add(objectMapper.createObjectNode().put("type", 3)
//                        .put("title", "声音简介")
//                        .set("detail", objectMapper.createArrayNode()
//                .add(objectMapper.createObjectNode().put("type", 1)
//                        .put("content", "123123123123213")
//                        .put("publishTime", 0)
//                        .put("orderNo", 1))))
        );
        objectNode.put("privacy", (String) queryMap.get("privacy"));
        objectNode.put("publishTime", "0");
        objectNode.put("orderNo", "1");
        ArrayNode arrayNode = objectMapper.createArrayNode().add(objectNode);
        node.put("voiceData", arrayNode.toString());
        node.put("dupkey", UUID.randomUUID().toString().replaceAll("\"\"", "\""));

        /*
        {
    "voiceId": 2545655464,
    "voiceName": "【阿梓歌】《心墙》（2023.11.27）",
    "categoryId": 2001,
    "categoryName": "创作翻唱",
    "secondCategoryId": 6175,
    "secondCategoryName": "歌曲翻唱",
    "voiceListId": 994819294,
    "voiceListName": "测试",
    "description": "编码:flac\n码率:14kbps->320kbps\n采样率:48000hz->48000hz\n视频bvid: BV1WG411S7tx\nb站作者: YiPLusDa\ngithub: nooblong/bilibili-to-netease-cloud-music",
    "composedSongs": "",
    "musicStampInfoList": null,
    "musicStampTaskId": 0,
    "publishTime": 1717576493333,
    "trackId": 2163634233,
    "dfsId": 509951163321135170,
    "privacy": false,
    "coverUrl": "http://p1.music.126.net/Wd-dmhB6VGljLJC-PM_m9w==/109951169657797671.jpg",
    "coverImgId": "109951169657797671",
    "createTime": 0,
    "displayStatus": "AUDIT_FAILED",
    "modules": "[{\"textList\":[{\"text\":\"编码:flac\"}]},{\"textList\":[{\"text\":\"码率:14kbps->320kbps\"}]},{\"textList\":[{\"text\":\"采样率:48000hz->48000hz\"}]},{\"textList\":[{\"text\":\"视频bvid: BV1WG411S7tx\"}]},{\"textList\":[{\"text\":\"b站作者: YiPLusDa\"}]},{\"textList\":[{\"text\":\"github: nooblong/bilibili-to-netease-cloud-music\"}]}]",
    "duration": 0,
    "feeVoice": false,
    "feeVoiceList": false,
    "price": 0,
    "startPoint": 0,
    "endPoint": 0,
    "rssEditorTips": false,
    "id": 2545655464,
    "name": "【阿梓歌】《心墙》（2023.11.27）",
    "publishType": 0,
    "relatedSongs": "[]",
    "csrf_token": "397c0481d550af613dda0b2eeb26899a"
    }
         */
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/api/voice/workbench/voice/update";
    }
}
