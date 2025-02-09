package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.UploadStatusTypeEnum;
import github.nooblong.download.api.AddQueueRequest;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.entity.UserVoicelist;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.service.UserVoicelistService;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/uploadDetail")
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

    @GetMapping("/listVoicelist")
    public Result<List<UserVoicelist>> listVoicelist(@RequestParam(name = "username", required = false) String username) {
        List<SysUser> userList = Db.list(Wrappers.lambdaQuery(SysUser.class)
                .eq(username != null, SysUser::getUsername, username));
        if (userList.isEmpty()) {
            return Result.ok("ok", new ArrayList<>());
        }
        List<UserVoicelist> list = Db.list(Wrappers.lambdaQuery(UserVoicelist.class)
                .in(UserVoicelist::getUserId, userList.stream().map(SysUser::getId).collect(Collectors.toList())));
        return Result.ok("ok", list);
    }

    @GetMapping("/refreshVoiceList")
    public Result<String> refreshVoiceList() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        userVoicelistService.syncUserVoicelist(sysUser.getId());
        return Result.ok("ok");
    }

    @GetMapping("/list")
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
        wrapper.like(StrUtil.isNotBlank(uploadName), UploadDetail::getUploadName, title);
        if (StrUtil.isNotBlank(username)) {
            LambdaQueryWrapper<SysUser> like = Wrappers.lambdaQuery(SysUser.class).like(SysUser::getUsername, username);
            List<SysUser> list = SimpleQuery.list(like, i -> i);
            if (!list.isEmpty()) {
                wrapper.eq(UploadDetail::getUserId,
                        list.get(0).getId());
                List<Subscribe> subscribes = Db.list(Wrappers.lambdaQuery(Subscribe.class)
                        .eq(Subscribe::getUserId, list.get(0).getId())
                        .select(Subscribe::getUpName, Subscribe::getId));
                subscribeMap = SimpleQuery.list2Map(subscribes, Subscribe::getId, i -> i);
            }
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
        }
        return Result.ok("查询成功", page);
    }

    @GetMapping("/getById")
    public Result<UploadDetail> get(@RequestParam(name = "id") Long id) {
        return Result.ok("ok", Db.getById(id, UploadDetail.class));
    }

    @PostMapping("/edit")
    public Result<UploadDetail> update(@RequestBody UploadDetail uploadDetail) {
        Long userId = JwtUtil.verifierFromContext().getId();
        UploadDetail byId = uploadDetailService.getById(uploadDetail.getId());
        Assert.isTrue(byId.getUserId().equals(userId), "assert error");
        uploadDetailService.updateById(byId);
        return Result.ok("ok", byId);
    }

    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam(name = "id") Long id) {
        Long userId = JwtUtil.verifierFromContext().getId();
        UploadDetail byId = uploadDetailService.getById(id);
        Assert.isTrue(byId.getUserId().equals(userId), "assert error");
        uploadDetailService.removeById(id);
        return Result.ok("ok");
    }

    @PostMapping("/add")
    public Result<String> addQueue(@RequestBody AddQueueRequest reqs) {
        Long userId = JwtUtil.verifierFromContext().getId();
        List<SysUser> userList = SimpleQuery.list(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getId, userId), i -> i);
        boolean isAdmin = !userList.isEmpty() && userList.get(0).getIsAdmin() == 1;
        for (UploadDetail req : reqs.getUploadDetails()) {
            Assert.isTrue(StrUtil.isNotBlank(req.getBvid()), "bvid empty");
            Assert.isTrue(req.getVoiceListId() != null, "voiceListId empty");
            SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(req.getBvid());
            UploadDetail uploadDetail = new UploadDetail();
            uploadDetail.setBvid(simpleVideoInfo.getBvid());
            uploadDetail.setCid(req.getCid());
            uploadDetail.setVoiceListId(req.getVoiceListId());
            uploadDetail.setUseVideoCover(req.getUseVideoCover());
            uploadDetail.setBeginSec(req.getBeginSec());
            uploadDetail.setEndSec(req.getEndSec());
            uploadDetail.setOffset(req.getOffset());
            uploadDetail.setUploadName(req.getUploadName());
            uploadDetail.setPrivacy(req.getPrivacy());
            uploadDetail.setPriority(isAdmin ? 999L : 10L);
            uploadDetail.setUserId(userId);
            uploadDetail.setCrack(req.getCrack());
            if (req.getCrack() == 1) {
                Assert.isTrue(isAdmin, "crack error");
            }
            Db.save(uploadDetail);
        }
        return Result.ok("添加队列成功");
    }

    @PostMapping("/restartJob")
    public Result<String> restartJob(@RequestParam(name = "id") Long id) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        UploadDetail byId = uploadDetailService.getById(id);
        Assert.notNull(byId, "null id");
        Assert.isTrue(sysUser.getId().equals(byId.getUserId()), "assert error");
        byId.setUploadRetryTimes(0);
        byId.setMusicRetryTimes(0);
        byId.setUploadStatus(UploadStatusTypeEnum.WAIT);
        uploadDetailService.updateById(byId);
        return Result.ok("ok");
    }

}
