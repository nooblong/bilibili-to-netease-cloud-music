package github.nooblong.btncm.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.service.IUserService;
import github.nooblong.btncm.enums.UploadStatusTypeEnum;
import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.job.GetUpJob;
import github.nooblong.btncm.job.UploadJob;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.utils.Constant;
import github.nooblong.btncm.service.UserVoicelistService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 配置里的定时任务从这里开始执行
 */
@Configuration
@Slf4j
public class ScheduleTask {

    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final SubscribeService subscribeService;
    final BilibiliClient bilibiliClient;
    final IUserService userService;
    final GetUpJob getUpJob;
    final UserVoicelistService userVoicelistService;
    final ObjectProvider<UploadJob> uploadJobProvider;

    public ScheduleTask(NetMusicClient netMusicClient,
                        UploadDetailService uploadDetailService,
                        SubscribeService service,
                        BilibiliClient bilibiliClient,
                        IUserService userService,
                        GetUpJob getUpJob,
                        UserVoicelistService userVoicelistService,
                        ObjectProvider<UploadJob> uploadJobProvider) {
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.subscribeService = service;
        this.bilibiliClient = bilibiliClient;
        this.userService = userService;
        this.getUpJob = getUpJob;
        this.userVoicelistService = userVoicelistService;
        this.uploadJobProvider = uploadJobProvider;
    }

    @Value("${enableUploadJob}")
    private int enableUploadJob;
    @Value("${enableGetAuditStatus}")
    private int enableGetAuditStatus;
    @Value("${enableGetUpJob}")
    private int enableGetUpJob;
    @Value("${enableRestartJob}")
    private int enableRestartJob;
    @Value("${enableRefreshNetCookie}")
    private int enableRefreshNetCookie;
    @Value("${enableRefreshBiliCookie}")
    private int enableRefreshBiliCookie;
    @Value("${enableRefreshUserVoiceList}")
    private int enableRefreshUserVoiceList;
    @Value("${removeUselessCookie}")
    private int removeUselessCookie;

    @Scheduled(fixedDelay = 10800, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getAuditStatus() {
        if (enableGetAuditStatus <= 0) {
            return;
        }
        uploadDetailService.checkAllAuditStatus();
    }

    @Scheduled(fixedDelay = 86400, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshUserVoiceList() {
        if (enableRefreshUserVoiceList <= 0) {
            return;
        }
        userVoicelistService.syncUserVoicelist();
    }

    @Scheduled(fixedDelay = 30800, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void removeUselessCookie() {
        if (removeUselessCookie <= 0) {
            return;
        }
        subscribeService.removeUselessCookie();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getUpJob() {
        if (enableGetUpJob <= 0) {
            return;
        }
        getUpJob.process();
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void uploadJob() {
        if (enableUploadJob <= 0) {
            return;
        }
        UploadDetail upload = uploadDetailService.getToUploadWithCookie();
        if (upload == null) {
            return;
        }
        log.info("处理: {}", upload.getTitle());
        Map<String, String> availableBilibiliCookie;
        try {
            availableBilibiliCookie = bilibiliClient.getBilibiliCookie();
        } catch (RuntimeException e) {
            log.error("b站cookie获取失败:", e);
            return;
        }
        UploadJob job = uploadJobProvider.getObject(upload.getId(), availableBilibiliCookie);
        job.start();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshNetCookie() {
        if (enableRefreshNetCookie <= 0) {
            return;
        }
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (StrUtil.isNotBlank(sysUser.getNetCookies())) {
                netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginRefresh", sysUser.getId());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @PostConstruct
    public void restartJob() {
        if (enableRestartJob <= 0) {
            return;
        }
        Path path = Paths.get(Constant.TMP_FOLDER);
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(file -> {
                        if (!file.equals(path.toFile())) {
                            boolean delete = file.delete();
                        }
                    });
            log.info("删除下载文件成功");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<UploadDetail> list = Db.list(Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.PROCESSING.name()));
        if (!list.isEmpty()) {
            list.forEach(i -> i.setUploadStatus(UploadStatusTypeEnum.WAIT));
            list.forEach(i -> log.info("重启任务: {} {}", i.getTitle(), i.getUploadName()));
            Db.updateBatchById(list);
        }
    }

    @Scheduled(fixedDelay = 3600, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshBiliCookie() {
        if (enableRefreshBiliCookie <= 0) {
            return;
        }
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (!sysUser.getBiliCookies().isBlank()) {
                try {
                    JsonNode need = bilibiliClient.checkRefresh(userService.getBilibiliCookieMap(sysUser.getId()));
                    if (need.get("data").asText().equals("false")) {
                        log.info("{}的b站cookie无需更新: {}", sysUser.getUsername(), sysUser.getUsername());
                    } else if (need.get("data").asText().equals("true")) {
                        log.info("{}的b站cookie需更新: {}", sysUser.getUsername(), sysUser.getUsername());
                        JsonNode refresh = bilibiliClient.refresh(userService.getBilibiliCookieMap(sysUser.getId()));
                        String sessdata = refresh.get("data").get("sessdata").asText();
                        String bili_jct = refresh.get("data").get("bili_jct").asText();
                        String dedeuserid = refresh.get("data").get("dedeuserid").asText();
                        String ac_time_value = refresh.get("data").get("ac_time_value").asText();
                        Map<String, String> updateMap = new HashMap<>();
                        updateMap.put("sessdata", sessdata);
                        updateMap.put("bili_jct", bili_jct);
                        updateMap.put("dedeuserid", dedeuserid);
                        updateMap.put("ac_time_value", ac_time_value);
                        userService.updateBilibiliCookieByCookieMap(sysUser.getId(), updateMap);
                    }
                } catch (Exception e) {
                    log.error(sysUser.getUsername() + "的b站cookie刷新失败: ", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * 第二天重置用户上传数量
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runAtMidnight() {
        userService.update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getVisitToday, 0)
                .set(SysUser::getVisitTodayTimes, 0)
                .set(SysUser::getRemaining, 100));
    }
}
