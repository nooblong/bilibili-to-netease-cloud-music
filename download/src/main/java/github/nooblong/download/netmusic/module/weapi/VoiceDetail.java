package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VoiceDetail extends SimpleWeApiModule {
    // return
    // "voiceId": 2529043319,
    //        "voiceName": "zhunitiantiankuaile",
    //        "categoryId": 2001,
    //        "categoryName": "创作翻唱",
    //        "secondCategoryId": 6171,
    //        "secondCategoryName": "原创demo",
    //        "voiceListId": 986001671,
    //        "voiceListName": "1",
    //        "description": "1",
    //        "composedSongs": null,
    //        "publishTime": 1693881906350,
    //        "trackId": 2079552644,
    //        "dfsId": 509951163305090362,
    //        "privacy": true,
    //        "coverUrl": "http://p1.music.126.net/ahHKwIQuy9B2DG505MsfrA==/109951168487511100.jpg",
    //        "coverImgId": "109951168487511100",
    //        "createTime": 0,
    //        "displayStatus": "ONLINE",
    //        "modules": null,
    //        "duration": 32182,
    //        "feeVoice": false,
    //        "feeVoiceList": false,
    //        "price": 0,
    //        "startPoint": 0,
    //        "endPoint": 0

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("id", (String) queryMap.get("id"));
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/voice/detail";
    }
}
