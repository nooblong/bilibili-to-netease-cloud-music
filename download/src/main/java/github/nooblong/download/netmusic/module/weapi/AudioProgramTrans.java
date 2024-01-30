package github.nooblong.download.netmusic.module.weapi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.download.netmusic.module.base.SimpleWeApiModule;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AudioProgramTrans extends SimpleWeApiModule {

    @Override
    public void genParams(ObjectNode node, Map<String, Object> queryMap) {
        node.put("position", (String) queryMap.get("position"));//最小为1，最大为歌曲数量，超过最大则为最底，小于1报错
        node.put("programId", Long.parseLong((String) queryMap.get("programId")));//2532916453 voiceId
        node.put("radioId", Long.parseLong((String) queryMap.get("radioId")));//996517258 /djradio?id=996517258 电台id
    }

    @Override
    public String getUrl() {
        return "https://interface.music.163.com/weapi/voice/workbench/radio/program/trans";
    }
}
