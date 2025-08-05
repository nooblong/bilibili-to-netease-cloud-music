package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.IdName;
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

    @GetMapping
    public Result<IPage<Subscribe>> list(@RequestParam(name = "username", required = false) String username,
                                         @RequestParam(name = "status", required = false) Integer status,
                                         @RequestParam(name = "voiceListId", required = false) Long voiceListId,
                                         HttpServletResponse response) {
        Long selectUserId = null;
        if (StrUtil.isNotBlank(username)) {
            List<SysUser> selectUser = Db.list(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, username));
            if (selectUser.isEmpty()) {
                return Result.ok("ok", new Page<>(1L, 1000L));
            }
            selectUserId = selectUser.get(0).getId();
        }
        IPage<Subscribe> list = Db.page(new Page<>(1, Integer.MAX_VALUE),
                Wrappers.lambdaQuery(Subscribe.class)
                        .eq(selectUserId != null, Subscribe::getUserId, selectUserId)
                        .eq(voiceListId != null, Subscribe::getVoiceListId, voiceListId)
                        .eq(status != null, Subscribe::getEnable, status)
                        .orderByDesc(Subscribe::getUpdateTime));
        if (list.getRecords().isEmpty()) {
            return Result.ok("ok", list);
        }
        List<SysUser> users = Db.list(SysUser.class);
        Map<Long, SysUser> longSysUserMap = SimpleQuery.list2Map(users, SysUser::getId, i -> i);
        list.getRecords().forEach(subscribe -> {
            subscribe.setTypeDesc(subscribe.getType().getDesc());
            subscribe.setUserName(longSysUserMap.get(subscribe.getUserId()).getUsername());
            subscribe.setLog(subscribe.getLog());
        });
        response.addHeader("Content-Range", String.valueOf(Db.count(Subscribe.class)));
        return Result.ok("ok", list);
    }

    @PutMapping
    public Result<Subscribe> edit(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(subscribe.getId(), Subscribe.class);
        if (subscribe.getCrack() != null && subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.updateById(subscribe);
        return Result.ok("ok", subscribe);
    }

    @PostMapping
    public Result<Subscribe> add(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        Long voiceListId = subscribe.getVoiceListId();
        Assert.notNull(voiceListId, "fail: voiceListId is null");
        List<UserVoicelist> list = userVoicelistService.lambdaQuery()
                .eq(UserVoicelist::getUserId, user.getId())
                .eq(UserVoicelist::getVoicelistId, voiceListId).list();
        Assert.isTrue(!list.isEmpty(), "fail: not owner");
        subscribe.setUserId(user.getId());
        if (subscribe.getCrack() != null && subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        List<IdName> channelIdsList = subscribe.getChannelIdsList();
        if (channelIdsList != null && !channelIdsList.isEmpty()) {
            ArrayList<String> sList = new ArrayList<>();
            channelIdsList.forEach(channelId -> sList.add(channelId.getId()));
            subscribe.setChannelIds(CommonUtil.toCommaSeparatedString(sList));
        }
        JsonNode userInfo = bilibiliClient.getUserInfo(subscribe.getUpId(),
                bilibiliClient.getAndSetBiliCookie());
        subscribe.setUpImage(userInfo.get("data").get("face").asText());
        subscribe.setUpName(userInfo.get("data").get("name").asText());
        if (channelIdsList != null && !channelIdsList.isEmpty()) {
            ArrayList<String> sList = new ArrayList<>();
            channelIdsList.forEach(channelId -> sList.add(channelId.getName()));
            String commaSeparatedString = CommonUtil.toCommaSeparatedString(sList);
            subscribe.setUpName(subscribe.getUpName() + "(" + commaSeparatedString + ")");
        }
        Db.save(subscribe);
        return Result.ok("ok", subscribe);
    }

    @DeleteMapping("{id}")
    public Result<Subscribe> delete(@PathVariable(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(id, Subscribe.class);
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.removeById(byId);
        return Result.ok("ok", byId);
    }

    @GetMapping("/{id}")
    public Result<Subscribe> subscribeList(@PathVariable(name = "id") Long id) {
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
