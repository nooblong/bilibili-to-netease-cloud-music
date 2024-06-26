package github.nooblong.download.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.job.GetUpJob;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class ScheduleTask {

    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final SubscribeService subscribeService;
    final BilibiliClient bilibiliClient;
    final IUserService userService;
    final GetUpJob getUpJob;
    final UploadJob uploadJob;

    public ScheduleTask(NetMusicClient netMusicClient,
                        UploadDetailService uploadDetailService,
                        SubscribeService service, BilibiliClient bilibiliClient,
                        IUserService userService,
                        GetUpJob getUpJob,
                        UploadJob uploadJob) {
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.subscribeService = service;
        this.bilibiliClient = bilibiliClient;
        this.userService = userService;
        this.getUpJob = getUpJob;
        this.uploadJob = uploadJob;
    }

    @Scheduled(fixedDelay = 10800, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getAuditStatus() {
        uploadDetailService.checkAllAuditStatus();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getUpJob() {
        getUpJob.process();
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void uploadJob() {
        uploadJob.uploadOne();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshNetCookie() {
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (StrUtil.isNotBlank(sysUser.getNetCookies())) {
                JsonNode loginrefresh = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginrefresh", sysUser.getId());
                log.info("用户 {} 网易cookie刷新结果: {}", sysUser.getUsername(), loginrefresh.toString());
            }
        }
    }

//    @Scheduled(fixedDelay = 3600, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshBiliCookie() {
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (!sysUser.getBiliCookies().isBlank()) {
                boolean need = bilibiliClient.needRefreshCookie(userService.getBilibiliCookieMap(sysUser.getId()));
                if (!need) {
                    log.info("b站cookie无需更新: {}", sysUser.getUsername());
                } else {
                    Map<String, String> refresh = bilibiliClient.refresh(userService.getBilibiliCookieMap(sysUser.getId()));
                    bilibiliClient.validate(refresh, sysUser.getId());
                }
            }
        }
    }
}
