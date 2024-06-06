package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.SysInfo;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys")
public class SystemController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final IUserService userService;

    public SystemController(BilibiliClient bilibiliClient,
                            NetMusicClient netMusicClient,
                            UploadDetailService uploadDetailService,
                            IUserService userService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.userService = userService;
    }

    @GetMapping("/sysInfo")
    public Result<SysInfo> sysInfo() {
        SysInfo sysInfo = new SysInfo();
        try {
            sysInfo.setReady(bilibiliClient.getAvailableBilibiliCookie() != null);
        } catch (RuntimeException e) {
            sysInfo.setReady(false);
        }
        try {
            SysUser sysUser = JwtUtil.verifierFromContext();
            boolean b = netMusicClient.checkLogin(sysUser.getId());
            sysInfo.setNetCookieStatus(b);

            if (StrUtil.isNotBlank(sysUser.getBiliCookies())) {
                Map<String, String> userCredMap = userService.getBilibiliCookieMap(sysUser.getId());
                boolean c = bilibiliClient.isLogin(userCredMap);
                sysInfo.setBilibiliCookieStatus(c);
            } else {
                sysInfo.setBilibiliCookieStatus(false);
            }
        } catch (Exception e) {
            sysInfo.setNetCookieStatus(false);
            sysInfo.setBilibiliCookieStatus(false);
        }
        long count = userService.count();
        sysInfo.setRegNum((int) count);

        SysUser anno = userService.getOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, "countNotRegisterUser"));
        if (anno != null) {
            sysInfo.setAnnoVisitNum(anno.getVisitTimes());
        }
        Integer sumVisitTime = userService.sumVisitTime();
        sysInfo.setUserVisitNum(sumVisitTime);
        return Result.ok("ok", sysInfo);
    }

    @GetMapping("/queueInfo")
    public Result<IPage<UploadDetail>> queueInfo(@RequestParam(name = "pageNo") int pageNo,
                                                 @RequestParam(name = "pageSize") int pageSize) {
        LambdaQueryWrapper<UploadDetail> queryWrapper = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getStatus, StatusTypeEnum.WAIT.name())
                .orderByDesc(UploadDetail::getPriority);
        LambdaQueryWrapper<UploadDetail> queryWrapper2 = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getStatus, StatusTypeEnum.PROCESSING.name())
                .orderByDesc(UploadDetail::getPriority);
        IPage<UploadDetail> page = uploadDetailService.page(new Page<>(pageNo, pageSize), queryWrapper);
        List<UploadDetail> list2 = uploadDetailService.list(queryWrapper2);
        if (!list2.isEmpty()) {
            list2.forEach(i -> {
                i.setUploadName(StrUtil.isNotBlank(i.getUploadName()) ? "处理中: " + i.getUploadName() : null);
                i.setTitle(StrUtil.isNotBlank(i.getTitle()) ? "处理中: " + i.getTitle() : null);
            });
            ArrayList<UploadDetail> uploadDetails = new ArrayList<>(page.getRecords());
            uploadDetails.addAll(0, list2);
            page.setRecords(uploadDetails);
        }
        page.getRecords().forEach(record -> record.setMergeTitle(StrUtil.isNotBlank(record.getUploadName()) ? record.getUploadName() : record.getTitle()));
        return Result.ok("ok", page);
    }

    @GetMapping("/log")
    public Result<String> log() {
        SysUser sysUser;
        try {
            sysUser = JwtUtil.verifierFromContext();
            sysUser.setVisitTimes(sysUser.getVisitTimes() + 1);
            Db.updateById(sysUser);
            return Result.ok("ok");
        } catch (Exception e) {
            LambdaQueryWrapper<SysUser> wrapper =
                    Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, "countNotRegisterUser");
            SysUser one = Db.getOne(wrapper);
            if (one == null) {
                Db.save(new SysUser().setUsername("countNotRegisterUser").setPassword("countNotRegisterUser"));
                one = Db.getOne(wrapper);
            }
            one.setVisitTimes(one.getVisitTimes() + 1);
            Db.updateById(one);
        }
        return Result.ok("ok");
    }

}
