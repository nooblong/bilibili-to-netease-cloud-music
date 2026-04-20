package github.nooblong.btncm.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.entity.Result;
import github.nooblong.btncm.utils.JwtUtil;
import github.nooblong.btncm.enums.UploadStatusTypeEnum;
import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.entity.CidName;
import github.nooblong.btncm.entity.Subscribe;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.entity.UserVoicelist;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.service.UserVoicelistService;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单曲上传接口
 */
@RestController
@RequestMapping("/upload")
public class UploadDetailController {

    final UploadDetailService uploadDetailService;

    final NetMusicClient netMusicClient;

    final BilibiliClient bilibiliClient;

    final UserVoicelistService userVoicelistService;

    public UploadDetailController(UploadDetailService uploadDetailService,
                                  NetMusicClient netMusicClient,
                                  BilibiliClient bilibiliClient,
                                  UserVoicelistService userVoicelistService) {
        this.uploadDetailService = uploadDetailService;
        this.netMusicClient = netMusicClient;
        this.bilibiliClient = bilibiliClient;
        this.userVoicelistService = userVoicelistService;
    }

    @GetMapping
    public Result<IPage<UploadDetail>> list(@RequestParam(name = "pageNo") int pageNo,
                                            @RequestParam(name = "pageSize") int pageSize,
                                            @RequestParam(name = "column", required = false) String column,
                                            @RequestParam(name = "orderBy", required = false) String orderBy,
                                            @RequestParam(required = false, name = "title") String title,
                                            @RequestParam(required = false, name = "voiceListId") String voiceListId,
                                            @RequestParam(required = false, name = "uploadName") String uploadName,
                                            @RequestParam(required = false, name = "username") String username,
                                            @RequestParam(required = false, name = "status") String status) {
        List<SysUser> users = Db.list(SysUser.class);
        Map<Long, SysUser> longSysUserMap = SimpleQuery.list2Map(users, SysUser::getId, i -> i);
        Map<Long, Subscribe> subscribeMap = new HashMap<>();

        IPage<UploadDetail> pageNew = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UploadDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(title), UploadDetail::getTitle, title);
        wrapper.like(StrUtil.isNotBlank(uploadName), UploadDetail::getUploadName, uploadName);
        if (StrUtil.isNotBlank(username)) {
            LambdaQueryWrapper<SysUser> like = Wrappers.lambdaQuery(SysUser.class).like(SysUser::getUsername, username);
            List<SysUser> list = SimpleQuery.list(like, i -> i);
            if (!list.isEmpty()) {
                wrapper.eq(UploadDetail::getUserId,
                        list.get(0).getId());
            } else {
                return Result.ok("查询成功", pageNew);
            }
        }
        List<Subscribe> subscribes = Db.list(Wrappers.lambdaQuery(Subscribe.class)
                .select(Subscribe::getUpName, Subscribe::getId));
        if (!subscribes.isEmpty()) {
            subscribeMap = SimpleQuery.list2Map(subscribes, Subscribe::getId, i -> i);
        }

        wrapper.like(StrUtil.isNotBlank(status), UploadDetail::getUploadStatus, status);
        wrapper.eq(StrUtil.isNotBlank(voiceListId), UploadDetail::getVoiceListId, voiceListId);

        if (StrUtil.isNotBlank(column) && StrUtil.isNotBlank(orderBy)) {
            if (orderBy.equalsIgnoreCase("desc")) {
                if (column.equalsIgnoreCase("createTime")) {
                    wrapper.orderByDesc(UploadDetail::getCreateTime);
                } else if (column.equalsIgnoreCase("updateTime")) {
                    wrapper.orderByDesc(UploadDetail::getUpdateTime);
                }
            } else {
                if (column.equalsIgnoreCase("createTime")) {
                    wrapper.orderByAsc(UploadDetail::getCreateTime);
                } else if (column.equalsIgnoreCase("updateTime")) {
                    wrapper.orderByAsc(UploadDetail::getUpdateTime);
                }
            }
        } else {
            wrapper.orderByDesc(UploadDetail::getId);
        }

        IPage<UploadDetail> page = uploadDetailService.page(pageNew, wrapper);

        for (UploadDetail record : page.getRecords()) {
            record.setUserName(longSysUserMap.get(record.getUserId()).getUsername());
            record.setStatusDesc(record.getUploadStatus().getDesc());
            record.setMergeTitle(StrUtil.isNotBlank(record.getUploadName()) ? record.getUploadName() :
                    record.getTitle());
            Subscribe subscribe = subscribeMap.get(record.getSubscribeId());
            record.setSubscribeName(subscribe == null ? "单曲上传" : subscribe.getUpName());
            record.setLog("");
        }
        return Result.ok("查询成功", page);
    }

    @PostMapping
    public Result<String> addQueue(@RequestBody UploadDetail req) {
        Long userId = JwtUtil.verifierFromContext().getId();
        List<SysUser> userList = SimpleQuery.list(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getId, userId), i -> i);
        boolean isAdmin = !userList.isEmpty() && userList.get(0).getIsAdmin() == 1;
        List<UserVoicelist> userVoicelistList = userVoicelistService.lambdaQuery()
                .eq(UserVoicelist::getUserId, userId)
                .list();
        Map<Long, UserVoicelist> userVoicelistMap =
                SimpleQuery.list2Map(userVoicelistList, UserVoicelist::getVoicelistId, i -> i);
        if (req.getCidNames() != null && !req.getCidNames().isEmpty()) {
            for (CidName cidName : req.getCidNames()) {
                Assert.isTrue(StrUtil.isNotBlank(req.getBvid()), "bvid为空");
                Assert.isTrue(req.getVoiceListId() != null && req.getVoiceListId() != 0, "voiceListId为空");
                Assert.notNull(userVoicelistMap.get(req.getVoiceListId()), "不是你的voiceListId");
                SimpleVideoInfo simpleVideoInfo = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(req.getBvid());
                UploadDetail uploadDetail = new UploadDetail();
                uploadDetail.setBvid(simpleVideoInfo.getBvid());
                uploadDetail.setCid(cidName.getCid());
                uploadDetail.setVoiceListId(req.getVoiceListId());
                uploadDetail.setUseVideoCover(req.getUseVideoCover());
                uploadDetail.setBeginSec(req.getBeginSec());
                uploadDetail.setEndSec(req.getEndSec());
                uploadDetail.setOffset(req.getOffset());
                uploadDetail.setUploadName(cidName.getName());
                uploadDetail.setPrivacy(req.getPrivacy());
                uploadDetail.setPriority(isAdmin ? 200L : 10L);
                uploadDetail.setBitrate(req.getBitrate());
                uploadDetail.setUserId(userId);
                uploadDetail.setCrack(req.getCrack() == null ? 0L : 1L);
                Db.save(uploadDetail);
            }
        } else {
            throw new RuntimeException("没有分p信息");
        }
        return Result.ok("添加队列成功");
    }

    @PutMapping
    public Result<UploadDetail> update(@RequestBody UploadDetail uploadDetail) {
        Long userId = JwtUtil.verifierFromContext().getId();
        UploadDetail byId = uploadDetailService.getById(uploadDetail.getId());
        Assert.isTrue(byId.getUserId().equals(userId), "assert error");
        uploadDetailService.updateById(byId);
        return Result.ok("ok", byId);
    }

    @GetMapping("/listVoicelist")
    public Result<IPage<UserVoicelist>> listVoicelist(@RequestParam(name = "username", required = false) String username,
                                                      @RequestParam(name = "pageNo", defaultValue = "1", required = false) Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10", required = false) Integer pageSize) {
        List<SysUser> userList = Db.list(Wrappers.lambdaQuery(SysUser.class)
                .eq(username != null, SysUser::getUsername, username));
        if (userList.isEmpty()) {
            return Result.ok("ok", new Page<>());
        }
        IPage<UserVoicelist> page = Db.page(new Page<>(pageNo, pageSize),
                Wrappers.lambdaQuery(UserVoicelist.class)
                        .in(UserVoicelist::getUserId, userList.stream().map(SysUser::getId).collect(Collectors.toList()))
                        .orderByDesc(UserVoicelist::getVoicelistId));
        for (UserVoicelist userVoicelist : page.getRecords()) {
            long count = Db.count(Wrappers.lambdaQuery(UploadDetail.class)
                    .eq(UploadDetail::getVoiceListId, userVoicelist.getVoicelistId()));
            userVoicelist.setUploadCount((int) count);
            long count2 = Db.count(Wrappers.lambdaQuery(Subscribe.class)
                    .eq(Subscribe::getVoiceListId, userVoicelist.getVoicelistId()));
            userVoicelist.setSubscribeNum((int) count2);
        }
        return Result.ok("ok", page);
    }

    @GetMapping("/refreshVoiceList")
    public Result<String> refreshVoiceList() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        userVoicelistService.syncUserVoicelist(sysUser.getId());
        return Result.ok("ok");
    }

    @GetMapping("/getById")
    public Result<UploadDetail> get(@RequestParam(name = "id") Long id) {
        return Result.ok("ok", Db.getById(id, UploadDetail.class));
    }

    @GetMapping("/delete")
    public Result<Boolean> delete(@RequestParam(name = "id") Long id) {
        Long userId = JwtUtil.verifierFromContext().getId();
        UploadDetail byId = uploadDetailService.getById(id);
        Assert.isTrue(byId.getUserId().equals(userId), "assert error");
        uploadDetailService.removeById(id);
        return Result.ok("ok");
    }

    @GetMapping("/restartJob")
    public Result<String> restartJob(@RequestParam(name = "id") Long id) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        UploadDetail byId = uploadDetailService.getById(id);
        Assert.notNull(byId, "null id");
        Assert.isTrue(sysUser.getId().equals(byId.getUserId()), "assert error");
        byId.setUploadRetryTimes(0);
        byId.setMusicRetryTimes(0);
        byId.setLog("");
        byId.setUploadStatus(UploadStatusTypeEnum.WAIT);
        uploadDetailService.updateById(byId);
        return Result.ok("ok");
    }

    @GetMapping("/delAllWait")
    public Result<String> delAllWait(@RequestParam(name = "voicelistId") Long voiceListId) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        uploadDetailService.remove(Wrappers.<UploadDetail>lambdaQuery()
                .eq(UploadDetail::getUserId, sysUser.getId())
                .eq(UploadDetail::getVoiceListId, voiceListId)
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.WAIT));
        return Result.ok("ok");
    }

    @GetMapping("/getLog")
    public Result<String> getLog(@RequestParam(name = "id") Long id) {
        UploadDetail byId = uploadDetailService.getById(id);
        return Result.ok("ok", byId.getLog());
    }

}
