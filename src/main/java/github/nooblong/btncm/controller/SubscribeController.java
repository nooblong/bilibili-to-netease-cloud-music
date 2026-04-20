package github.nooblong.btncm.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.btncm.bilibili.*;
import github.nooblong.btncm.entity.*;
import github.nooblong.btncm.enums.SubscribeTypeEnum;
import github.nooblong.btncm.enums.VideoOrderEnum;
import github.nooblong.btncm.utils.CommonUtil;
import github.nooblong.btncm.utils.JwtUtil;
import github.nooblong.btncm.enums.UserVideoOrderEnum;
import github.nooblong.btncm.job.UploadJob;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.service.UserVoicelistService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订阅接口
 */
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

    @PutMapping("/{id}")
    public Result<Subscribe> edit(@RequestBody Subscribe subscribe, @PathVariable Long id) {
        subscribe.setId(id);
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(subscribe.getId(), Subscribe.class);
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
        List<IdName> channelIdsList = subscribe.getChannelIdsList();
        if (channelIdsList != null && !channelIdsList.isEmpty()) {
            ArrayList<String> sList = new ArrayList<>();
            channelIdsList.forEach(channelId -> sList.add(channelId.getId()));
            subscribe.setChannelIds(CommonUtil.toCommaSeparatedString(sList));
        }
        JsonNode userInfo = bilibiliClient.getUserInfo(subscribe.getUpId(),
                bilibiliClient.getBilibiliCookie());
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
        Db.remove(Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getSubscribeId, byId.getId())
                .eq(UploadDetail::getUploadStatus, "WAIT"));
        return Result.ok("ok", byId);
    }

    @GetMapping("/{id}")
    public Result<Subscribe> subscribeList(@PathVariable(name = "id") Long id) {
        Subscribe byId = Db.getById(id, Subscribe.class);
        return Result.ok("ok", byId);
    }

    @GetMapping("/checkMyUpJob")
    public Result<String> checkMyUpJob(@RequestParam("voicelistId") Long voiceListId) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        subscribeService.checkAndSave(sysUser.getId(), voiceListId);
        return Result.ok("ok");
    }

    @GetMapping("/test")
    public Result<List<String>> test(@RequestParam("subscribeId") Long subscribeId) {
        int times = 10;
        Subscribe subscribe = Db.getById(subscribeId, Subscribe.class);
        List<UploadDetail> uploadDetails = new ArrayList<>();
        List<String> result = new ArrayList<>();
        Map<String, String> availableBilibiliCookie = bilibiliClient.getBilibiliCookie();
        if (subscribe.getType() == SubscribeTypeEnum.UP) {
            UpIterator upIterator = new UpIterator(bilibiliClient, subscribe.getUpId(), subscribe.getKeyWord(),
                    subscribe.getLimitSec(), subscribe.getMinSec(), VideoOrderEnum.valueOf(subscribe.getVideoOrder()),
                    UserVideoOrderEnum.PUBDATE, subscribe.getCheckPart() == 1,
                    availableBilibiliCookie, subscribe.getLastTotalIndex(), subscribe.getChannelIds(), new AtomicInteger(-1));
            uploadDetails = subscribeService.testProcess(subscribe, upIterator, times);
        }
        if (subscribe.getType() == SubscribeTypeEnum.FAVORITE) {
            String favIds = subscribe.getChannelIds();
            List<String> favIdList = CommonUtil.toList(favIds);
            for (String favId : favIdList) {
                FavoriteIterator favIterator = new FavoriteIterator(favId, bilibiliClient,
                        subscribe.getLimitSec(), subscribe.getMinSec(), subscribe.getCheckPart() == 1,
                        availableBilibiliCookie);
                List<UploadDetail> partDetails = subscribeService.testProcess(subscribe, favIterator, times);
                uploadDetails.addAll(partDetails);
            }
        }
        if (!uploadDetails.isEmpty()) {
            Map<String, String> cookie = bilibiliClient.getBilibiliCookie();
            for (UploadDetail uploadDetail : uploadDetails) {
                SimpleVideoInfo video = new SimpleVideoInfo();
                video.setBvid(uploadDetail.getBvid());
                video.setCid(uploadDetail.getCid());
                BilibiliFullVideo fullVideo = bilibiliClient.getFullVideoBySimpleVideo(video, cookie);
                String regName = subscribe.getRegName();
                String s = UploadJob.handleUploadName(regName, uploadDetail, fullVideo);
                result.add(s);
            }
            return Result.ok("ok", result);
        }
        return Result.ok("ok", result);
    }

}
