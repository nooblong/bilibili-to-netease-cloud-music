package github.nooblong.download.controller;

import github.nooblong.common.model.Result;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.SysInfo;
import github.nooblong.download.entity.WorkerStatus;
import github.nooblong.download.job.JobUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SystemController {

    final BilibiliClient bilibiliClient;

    public SystemController(BilibiliClient bilibiliClient) {
        this.bilibiliClient = bilibiliClient;
    }

    @GetMapping("/sysInfo")
    public Result<SysInfo> sysInfo() {
        List<WorkerStatus> workerStatusList = JobUtil.workerStatusList();
        SysInfo sysInfo = new SysInfo();
        sysInfo.setWorkerStatusList(workerStatusList);
        sysInfo.setActiveBilibiliUserName(bilibiliClient.getCurrentUser() != null
                ? bilibiliClient.getCurrentUser().getUsername()
                : "no bilibili cookie, stop the world!");
        return Result.ok("ok", sysInfo);
    }

}
