package github.nooblong.download.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.model.Result;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.SysInfo;
import github.nooblong.download.job.JobUtil;
import github.nooblong.download.mq.MusicQueue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SystemController {

    final BilibiliClient bilibiliClient;
    final MusicQueue musicQueue;

    public SystemController(BilibiliClient bilibiliClient,
                            MusicQueue musicQueue) {
        this.bilibiliClient = bilibiliClient;
        this.musicQueue = musicQueue;
    }

    @GetMapping("/sysInfo")
    public Result<SysInfo> sysInfo() {
        List<WorkerStatus> workerStatusList = JobUtil.workerStatusList();
        SysInfo sysInfo = new SysInfo();
        sysInfo.setWorkerStatusList(workerStatusList);
        sysInfo.setActiveBilibiliUserName(bilibiliClient.getCurrentUser() != null
                ? bilibiliClient.getCurrentUser().getUsername()
                : null);
        return Result.ok("ok", sysInfo);
    }

    @GetMapping("/queueInfo")
    public Result<JsonNode> queueInfo() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("id", 1);
        objectNode.set("data", objectMapper.valueToTree(musicQueue.listAllQueue()));
        return Result.ok("ok", objectNode);
    }

}
