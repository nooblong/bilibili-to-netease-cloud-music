package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VoiceDetail extends SimpleWeApiModule {
    // return
    /*
    {
    "code": 200,
    "message": null,
    "data": {
        "voiceId": 2545655464,
        "voiceName": "【阿梓歌】《心墙》（2023.11.27）",
        "categoryId": 2001,
        "categoryName": "创作翻唱",
        "secondCategoryId": 6175,
        "secondCategoryName": "歌曲翻唱",
        "voiceListId": 994819294,
        "voiceListName": "测试",
        "description": "编码:flac\n码率:14kbps->320kbps\n采样率:48000hz->48000hz\n视频bvid: BV1WG411S7tx\nb站作者: YiPLusDa\ngithub: nooblong/bilibili-to-netease-cloud-music",
        "composedSongs": null,
        "musicStampInfoList": null,
        "musicStampTaskId": 0,
        "publishTime": 1717576493333,
        "trackId": 2163634233,
        "dfsId": 509951163321135170,
        "privacy": false,
        "coverUrl": "http://p1.music.126.net/Wd-dmhB6VGljLJC-PM_m9w==/109951169657797671.jpg",
        "coverImgId": "109951169657797671",
        "createTime": 0,
        "displayStatus": "ONLINE",
        "modules": [
            {
                "textList": [
                    {
                        "text": "编码:flac",
                        "attributes": null
                    }
                ],
                "attributes": null
            },
            {
                "textList": [
                    {
                        "text": "码率:14kbps->320kbps",
                        "attributes": null
                    }
                ],
                "attributes": null
            },
            {
                "textList": [
                    {
                        "text": "采样率:48000hz->48000hz",
                        "attributes": null
                    }
                ],
                "attributes": null
            },
            {
                "textList": [
                    {
                        "text": "视频bvid: BV1WG411S7tx",
                        "attributes": null
                    }
                ],
                "attributes": null
            },
            {
                "textList": [
                    {
                        "text": "b站作者: YiPLusDa",
                        "attributes": null
                    }
                ],
                "attributes": null
            },
            {
                "textList": [
                    {
                        "text": "github: nooblong/bilibili-to-netease-cloud-music",
                        "attributes": null
                    }
                ],
                "attributes": null
            }
        ],
        "duration": 219336,
        "feeVoice": false,
        "feeVoiceList": false,
        "price": 0,
        "startPoint": 0,
        "endPoint": 0,
        "rssEditorTips": false
    }
}
     */

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("id"));
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voice/detail";
    }
}
