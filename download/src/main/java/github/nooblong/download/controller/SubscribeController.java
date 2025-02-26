package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UserVoicelist;
import github.nooblong.download.job.UploadJob;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.service.UserVoicelistService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subscribe")
public class SubscribeController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final SubscribeService subscribeService;
    final UserVoicelistService userVoicelistService;
    final UploadJob uploadJob;

    public SubscribeController(BilibiliClient bilibiliClient,
                               NetMusicClient netMusicClient,
                               UploadDetailService uploadDetailService,
                               SubscribeService subscribeService,
                               UserVoicelistService userVoicelistService,
                               UploadJob uploadJob) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.subscribeService = subscribeService;
        this.userVoicelistService = userVoicelistService;
        this.uploadJob = uploadJob;
    }

    @CacheEvict(value = {"subscribe/list", "subscribe/test"}, allEntries = true)
    @PostMapping("/edit")
    public Result<Subscribe> edit(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(subscribe.getId(), Subscribe.class);
        if (subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.updateById(subscribe);
        return Result.ok("ok", subscribe);
    }

    @CacheEvict(value = {"subscribe/list"}, allEntries = true)
    @PostMapping("/add")
    public Result<Subscribe> add(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        Long voiceListId = subscribe.getVoiceListId();
        Assert.notNull(voiceListId, "fail: voiceListId is null");
        List<UserVoicelist> list = userVoicelistService.lambdaQuery()
                .eq(UserVoicelist::getUserId, user.getId())
                .eq(UserVoicelist::getVoicelistId, voiceListId).list();
        Assert.isTrue(!list.isEmpty(), "fail: not owner");
        subscribe.setUserId(user.getId());
        if (subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        subscribe.setChannelIds(CommonUtil.toCommaSeparatedString(subscribe.getChannelIdsList()));
        JsonNode userInfo = bilibiliClient.getUserInfo(subscribe.getUpId(),
                bilibiliClient.getAndSetBiliCookie());
        subscribe.setUpImage(userInfo.get("data").get("face").asText());
        subscribe.setUpName(userInfo.get("data").get("name").asText());
        Db.save(subscribe);
        return Result.ok("ok", subscribe);
    }

    @CacheEvict(value = {"subscribe/list"}, allEntries = true)
    @GetMapping("/delete")
    public Result<Subscribe> delete(@RequestParam(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(id, Subscribe.class);
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.removeById(byId);
        return Result.ok("ok", byId);
    }

    @Cacheable(value = "subscribe/list", key = "#username + ':' + #status + ':' + #voiceListId")
    @GetMapping("/list")
    public Result<List<Subscribe>> subscribeList(@RequestParam(name = "username", required = false) String username,
                                                 @RequestParam(name = "status", required = false) Integer status,
                                                 @RequestParam(name = "voiceListId", required = false) Long voiceListId,
                                                 HttpServletResponse response) {
        Long selectUserId = null;
        if (StrUtil.isNotBlank(username)) {
            List<SysUser> selectUser = Db.list(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, username));
            if (selectUser.isEmpty()) {
                return Result.ok("ok", new ArrayList<>());
            }
            selectUserId = selectUser.get(0).getId();
        }
        List<Subscribe> list = Db.list(
                Wrappers.lambdaQuery(Subscribe.class)
                        .eq(selectUserId != null, Subscribe::getUserId, selectUserId)
                        .eq(voiceListId != null, Subscribe::getVoiceListId, voiceListId)
                        .eq(status != null, Subscribe::getEnable, status));
        if (list.isEmpty()) {
            return Result.ok("ok", list);
        }
        List<SysUser> users = Db.list(SysUser.class);
        Map<Long, SysUser> longSysUserMap = SimpleQuery.list2Map(users, SysUser::getId, i -> i);
        list.forEach(subscribe -> {
            subscribe.setTypeDesc(subscribe.getType().getDesc());
            subscribe.setUserName(longSysUserMap.get(subscribe.getUserId()).getUsername());
            subscribe.setLog(subscribe.getLog());
        });
        response.addHeader("Content-Range", String.valueOf(Db.count(Subscribe.class)));
        return Result.ok("ok", list);
    }

    @GetMapping("/detail")
    public Result<Subscribe> subscribeList(@RequestParam(name = "id") Long id) {
        Subscribe byId = Db.getById(id, Subscribe.class);
        return Result.ok("ok", byId);
    }

    @GetMapping("/checkUpJob")
    public Result<String> checkUpJob() {
        Assert.isTrue(JwtUtil.verifierFromContext().getIsAdmin() == 1, "fail: no permission");
        subscribeService.checkAndSave();
        return Result.ok("ok");
    }

    @GetMapping("/checkMyUpJob")
    public Result<String> checkMyUpJob(@RequestParam("voicelistId") Long voiceListId) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        subscribeService.checkAndSave(sysUser.getId(), voiceListId);
        return Result.ok("ok");
    }

    @Cacheable(value = "subscribe/test")
    @GetMapping("/test")
    public Result<List<String>> test(@RequestParam("subscribeId") Long subscribeId) {
        List<String> result = uploadJob.test(subscribeId);
        return Result.ok("ok", result);
    }

}
