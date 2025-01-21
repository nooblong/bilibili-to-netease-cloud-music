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
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

        IPage<UploadDetail> pageNew = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UploadDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(title), UploadDetail::getTitle, title);
        wrapper.like(StrUtil.isNotBlank(uploadName), UploadDetail::getUploadName, title);
        if (StrUtil.isNotBlank(username)) {
            LambdaQueryWrapper<SysUser> like = Wrappers.lambdaQuery(SysUser.class).like(SysUser::getUsername, username);
            List<SysUser> list = SimpleQuery.list(like, i -> i);
            wrapper.in(!list.isEmpty(), UploadDetail::getUserId,
                    list.stream().map(SysUser::getId).collect(Collectors.toList()));
        }
        wrapper.like(StrUtil.isNotBlank(status), UploadDetail::getStatus, status);

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
            record.setStatusDesc(record.getStatus().getDesc());
            record.setMergeTitle(StrUtil.isNotBlank(record.getUploadName()) ? record.getUploadName() :
                    record.getTitle());
            record.setLog(null);
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
    public Result<String> addQueue(@RequestBody List<AddQueueRequest> reqs) {
        Long userId = JwtUtil.verifierFromContext().getId();
        for (AddQueueRequest req : reqs) {
            Assert.isTrue(StrUtil.isNotBlank(req.getBvid()), "bvid empty");
            Assert.isTrue(req.getVoiceListId() != null, "voiceListId empty");
            SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(req.getBvid());
            UploadDetail uploadDetail = new UploadDetail();
            uploadDetail.setBvid(simpleVideoInfo.getBvid());
            uploadDetail.setCid(req.getCid());
            uploadDetail.setVoiceListId(req.getVoiceListId());
            uploadDetail.setUseVideoCover(req.getUseDefaultImg() != null && req.getUseDefaultImg() ? 1L : 0L);
            uploadDetail.setBeginSec(req.getVoiceBeginSec());
            uploadDetail.setEndSec(req.getVoiceEndSec());
            uploadDetail.setOffset(req.getVoiceOffset());
            uploadDetail.setUploadName(req.getUploadName());
            uploadDetail.setTitle(simpleVideoInfo.getTitle());
            uploadDetail.setPrivacy(req.getPrivacy() != null && req.getPrivacy() ? 1L : 0L);
            uploadDetail.setPriority(10L);
            uploadDetail.setUserId(userId);

            if (req.getCrack() != null && req.getCrack()) {
                List<SysUser> userList = SimpleQuery.list(Wrappers.lambdaQuery(SysUser.class)
                        .eq(SysUser::getId, userId), i -> i);
                Assert.isTrue(!userList.isEmpty() &&
                        userList.get(0).getIsAdmin() == 1, "assert error");
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
        byId.setRetryTimes(0);
        byId.setStatus(StatusTypeEnum.WAIT);
        uploadDetailService.updateById(byId);
        return Result.ok("ok");
    }

}
