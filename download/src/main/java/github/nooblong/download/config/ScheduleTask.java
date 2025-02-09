package github.nooblong.download.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.service.IUserService;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.job.GetUpJob;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.utils.Constant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelay = 10800, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void getAuditStatus() {
        if (enableGetAuditStatus <= 0) {
            return;
        }
        uploadDetailService.checkAllAuditStatus();
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
        uploadJob.uploadOne();
    }

    @Scheduled(fixedDelay = 7200, timeUnit = TimeUnit.SECONDS, initialDelayString = "${initialDelay}")
    public void refreshNetCookie() {
        if (enableRefreshNetCookie <= 0) {
            return;
        }
        List<SysUser> list = Db.list(SysUser.class);
        for (SysUser sysUser : list) {
            if (StrUtil.isNotBlank(sysUser.getNetCookies())) {
                JsonNode loginrefresh = netMusicClient.getMusicDataByUserId(new HashMap<>(), "loginrefresh", sysUser.getId());
                log.info("用户 {} 网易cookie刷新结果: {}", sysUser.getUsername(), loginrefresh.toString());
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
            System.out.println("删除下载文件成功");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<UploadDetail> list = Db.list(Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getStatus, StatusTypeEnum.PROCESSING.name()));
        if (!list.isEmpty()) {
            list.forEach(i -> i.setStatus(StatusTypeEnum.WAIT));
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
