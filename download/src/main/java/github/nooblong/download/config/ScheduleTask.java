package github.nooblong.download.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.download.bilibili.BilibiliUtil;
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
    final BilibiliUtil bilibiliUtil;

    public ScheduleTask(NetMusicClient netMusicClient,
                        UploadDetailService uploadDetailService,
                        SubscribeService service, BilibiliUtil bilibiliUtil) {
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.subscribeService = service;
        this.bilibiliUtil = bilibiliUtil;
    }

    @Scheduled(fixedDelay = 10800, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getAuditStatus() {
        uploadDetailService.checkAllAuditStatus();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getUpJob() {
        subscribeService.checkAndSendMessage();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshNetCookie() {
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (StrUtil.isNotBlank(sysUser.getNetCookies())) {
                JsonNode loginrefresh = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginrefresh", sysUser.getId());
                log.info("用户 {} cookie刷新结果: {}", sysUser.getUsername(), loginrefresh.toString());
            }
        }
    }

    @Scheduled(fixedDelay = 3600, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshBiliCookie() {
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (!sysUser.getBiliCookies().isBlank()) {
                boolean need = bilibiliUtil.needRefreshCookie(bilibiliUtil.getCurrentCred());
                if (!need) {
                    log.info("b站cookie无需更新: {}", sysUser.getUsername());
                } else {
                    Map<String, String> refresh = bilibiliUtil.refresh(bilibiliUtil.getCurrentCred());
                    bilibiliUtil.validate(refresh, sysUser.getId());
                }
            }
        }
    }

    @Scheduled(fixedDelay = 300, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void checkBilibiliCookie() {
        bilibiliUtil.checkCurrentCredMap();
    }

}
