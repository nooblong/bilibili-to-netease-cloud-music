package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.api.AddQueueRequest;
import github.nooblong.download.api.AddToMyRequest;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/uploadDetail")
public class UploadDetailController {
    final UploadDetailService uploadDetailService;
    final NetMusicClient netMusicClient;
    final BilibiliClient bilibiliClient;

    public UploadDetailController(UploadDetailService uploadDetailService,
                                  NetMusicClient netMusicClient,
                                  BilibiliClient bilibiliClient) {
        this.uploadDetailService = uploadDetailService;
        this.netMusicClient = netMusicClient;
        this.bilibiliClient = bilibiliClient;
    }

    @GetMapping("/{id}")
    public Result<UploadDetail> get(@PathVariable(name = "id") Long id) {
        return Result.ok("ok", Db.getById(id, UploadDetail.class));
    }

    @PutMapping("/{id}")
    public Result<UploadDetail> update(@PathVariable(name = "id") Long id, @RequestBody UploadDetail uploadDetail) {
        Long userId = JwtUtil.verifierFromContext().getId();
        UploadDetail byId = uploadDetailService.getById(id);
        if (!userId.equals(byId.getUserId())) {
            throw new RuntimeException("只能修改自己的");
        }
        byId.setUploadName(uploadDetail.getUploadName());
        uploadDetailService.updateById(byId);
        return Result.ok("ok", byId);
    }

    @PostMapping("/addQueue")
    public Result<String> addQueue(@RequestBody @Validated AddQueueRequest req) {
        Long userId = JwtUtil.verifierFromContext().getId();

        SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(req.getBvid());
        UploadDetail uploadDetail = new UploadDetail();
        uploadDetail.setBvid(simpleVideoInfo.getBvid());
        uploadDetail.setCid(req.getCid());
        uploadDetail.setVoiceListId(req.getVoiceListId());
        uploadDetail.setUseVideoCover(req.isUseDefaultImg() ? 1L : 0L);
        uploadDetail.setBeginSec(req.getVoiceBeginSec());
        uploadDetail.setEndSec(req.getVoiceEndSec());
        if (req.getVoiceBeginSec() != 0 && req.getVoiceEndSec() == 0) {
            uploadDetail.setEndSec(99999999D);
        }
        uploadDetail.setOffset(req.getVoiceOffset());
        uploadDetail.setUploadName(req.getCustomUploadName());
        uploadDetail.setTitle(simpleVideoInfo.getTitle());
        uploadDetail.setPrivacy(req.isPrivacy() ? 1L : 0L);
        uploadDetail.setPriority(10L);
        uploadDetail.setUserId(userId);

        if (req.isCrack()) {
            if (!userId.equals(1L)) {
                return Result.fail("暂不开放");
            } else {
                uploadDetail.setCrack(1L);
            }
        }

        Db.save(uploadDetail);
        return Result.ok("添加队列成功");
    }

    @PostMapping("/addToMyList")
    public Result<String> addToMyList(@RequestBody @Validated AddToMyRequest req, HttpServletRequest request) throws JsonProcessingException {
        UploadDetail uploadDetail = Db.getById(req.getVoiceDetailId(), UploadDetail.class);
        SysUser user = JwtUtil.verifierFromContext();
        if (uploadDetail == null) {
            return Result.fail("不存在的id");
        }
        UploadDetail addToMy = new UploadDetail();
        addToMy.setBvid(uploadDetail.getBvid());
        addToMy.setCid(uploadDetail.getCid());
        addToMy.setVoiceListId(Long.valueOf(req.getVoiceListId()));
        addToMy.setPriority(9L);
        addToMy.setTitle(uploadDetail.getTitle());
        addToMy.setUserId(user.getId());
        addToMy.setUploadName(uploadDetail.getUploadName());
        addToMy.setBeginSec(uploadDetail.getBeginSec());
        addToMy.setEndSec(uploadDetail.getEndSec());
        addToMy.setUseVideoCover(uploadDetail.getUseVideoCover());
        addToMy.setOffset(uploadDetail.getOffset());
        uploadDetailService.save(addToMy);
        return Result.ok("添加队列成功");
    }

    @GetMapping()
    public Result<IPage<UploadDetail>> recent(@RequestParam(name = "pageNo") int pageNo,
                                              @RequestParam(name = "pageSize") int pageSize,
                                              @RequestParam(required = false, name = "title") String title,
                                              @RequestParam(required = false, name = "remark") String remark,
                                              @RequestParam(required = false, name = "username") String username,
                                              @RequestParam(required = false, name = "status") String status) {
        List<SysUser> users = Db.list(SysUser.class);
        Map<Long, SysUser> longSysUserMap = SimpleQuery.list2Map(users, SysUser::getId, i -> i);

        IPage<UploadDetail> pageNew = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UploadDetail> wrapper = new LambdaQueryWrapper<UploadDetail>().orderByDesc(UploadDetail::getId);
        wrapper.like(title != null, UploadDetail::getUploadName, title);
        if (remark != null) {
            LambdaQueryWrapper<Subscribe> like = Wrappers.lambdaQuery(Subscribe.class).like(Subscribe::getRemark, remark);
            List<Subscribe> list = SimpleQuery.list(like, i -> i);
            if (list.isEmpty()) {
                return Result.ok("ok", new Page<>(0, 0, 0));
            }
            wrapper.in(UploadDetail::getVoiceListId, list.stream().map(Subscribe::getVoiceListId).collect(Collectors.toList()));
        }
        if (username != null) {
            LambdaQueryWrapper<SysUser> like = Wrappers.lambdaQuery(SysUser.class).like(SysUser::getUsername, username);
            List<SysUser> list = SimpleQuery.list(like, i -> i);
            if (list.isEmpty()) {
                return Result.ok("ok", new Page<>(0, 0, 0));
            }
            wrapper.in(UploadDetail::getUserId, list.stream().map(SysUser::getId).collect(Collectors.toList()));
        }
        if (status != null && status.equalsIgnoreCase("other")) {
            wrapper.notIn(UploadDetail::getStatus, StatusTypeEnum.ONLINE.name(),
                    StatusTypeEnum.ONLY_SELF_SEE.name(),
                    StatusTypeEnum.AUDITING.name());
        } else {
            wrapper.like(status != null, UploadDetail::getStatus, status);
        }
        IPage<UploadDetail> page = uploadDetailService.page(pageNew, wrapper);

        for (UploadDetail record : page.getRecords()) {
            record.setUserName(longSysUserMap.get(record.getUserId()).getUsername());
            record.setStatusDesc(record.getStatus().getDesc());
            record.setMergeTitle(StrUtil.isNotBlank(record.getUploadName()) ? record.getUploadName() : record.getTitle());
        }
        return Result.ok("查询成功", page);
    }

    @GetMapping("/checkHasUploaded")
    public Result<Boolean> checkHasUploaded() {
        return Result.ok("ok", uploadDetailService.hasUploaded(JwtUtil.verifierFromContext().getId()));
    }

    @GetMapping("/restartJob/{id}")
    public Result<String> restartJob(@PathVariable(name = "id") Long id) {
        SysUser sysUser = JwtUtil.verifierFromContext();
        UploadDetail byId = uploadDetailService.getById(id);
        Assert.notNull(byId, "空id");
        Assert.isTrue(sysUser.getId().equals(byId.getUserId()), "只能操作自己的");
        byId.setRetryTimes(0);
        byId.setStatus(StatusTypeEnum.WAIT);
        return Result.ok("ok");
    }

}
