package github.nooblong.download.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.service.IUserService;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.AfdUtil;
import github.nooblong.download.UploadStatusTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.AfdOrder;
import github.nooblong.download.entity.SysInfo;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.AfdOrderService;
import github.nooblong.download.service.UploadDetailService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys")
public class SystemController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final IUserService userService;
    final AfdOrderService afdOrderService;

    public SystemController(BilibiliClient bilibiliClient,
                            NetMusicClient netMusicClient,
                            UploadDetailService uploadDetailService,
                            IUserService userService,
                            AfdOrderService afdOrderService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.userService = userService;
        this.afdOrderService = afdOrderService;
    }

    @GetMapping("/sysInfo")
    public Result<SysInfo> sysInfo() {
        SysInfo sysInfo = new SysInfo();
        long count = userService
                .lambdaQuery().isNotNull(SysUser::getNetCookies)
                .ne(SysUser::getNetCookies, "").count();
        sysInfo.setLogin163Num((int) count);
        Integer visitTimes = userService.visitTimes();
        sysInfo.setVisitTimes(visitTimes);
        Integer visitToday = userService.visitToday();
        sysInfo.setVisitToday(visitToday);
        Integer visitTodayTimes = userService.visitTodayTimes();
        sysInfo.setVisitTodayTimes(visitTodayTimes);
        List<AfdOrder> orderList = afdOrderService.list(Wrappers.lambdaQuery(AfdOrder.class)
                .isNotNull(AfdOrder::getOutCreateTime));
        sysInfo.setAfdOrders(orderList);
        try {
            SysUser sysUser = JwtUtil.verifierFromContext();
            sysInfo.setLogin(true);
            sysInfo.setExpireTime(sysUser.getExpire());
            sysInfo.setAfdId(StrUtil.isBlank(sysUser.getAfdUserId()) ? "-" : sysUser.getAfdUserId());
            List<AfdOrder> myOrder =
                    orderList.stream().filter(i -> i.getUserId().equals(sysUser.getId())).toList();
            sysInfo.setMyOrders(myOrder);
        } catch (Exception e) {
            sysInfo.setExpireTime(DateUtil.parse("2000-01-01 00:00:00"));
            sysInfo.setLogin(false);
        }
        return Result.ok("ok", sysInfo);
    }

    @GetMapping("/queueInfo")
    public Result<IPage<UploadDetail>> queueInfo(@RequestParam(name = "pageNo") int pageNo,
                                                 @RequestParam(name = "pageSize") int pageSize) {
        LambdaQueryWrapper<UploadDetail> queryWrapper = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.WAIT.name())
                .orderByDesc(UploadDetail::getPriority)
                .orderByAsc(UploadDetail::getCreateTime);
        LambdaQueryWrapper<UploadDetail> queryWrapper2 = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.PROCESSING.name())
                .orderByDesc(UploadDetail::getPriority)
                .orderByAsc(UploadDetail::getCreateTime);
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
        page.getRecords().forEach(record -> {
            record.setMergeTitle(StrUtil.isNotBlank(record.getUploadName()) ? record.getUploadName() :
                    record.getTitle());
            record.setLog(null);
        });
        return Result.ok("ok", page);
    }

    @GetMapping("/log")
    public Result<String> log() {
        SysUser sysUser;
        try {
            sysUser = JwtUtil.verifierFromContext();
            sysUser.setVisitTimes(sysUser.getVisitTimes() + 1);
            if (sysUser.getVisitToday() == 0) {
                sysUser.setVisitToday(1);
            }
            sysUser.setVisitTodayTimes(sysUser.getVisitTodayTimes() + 1);
            Db.updateById(sysUser);
            return Result.ok("ok");
        } catch (Exception ignored) {

        }
        return Result.ok("ok");
    }

    @GetMapping("/generateOrder")
    public Result<String> generateOrder() {
        SysUser sysUser = JwtUtil.verifierFromContext();
        String orderId = afdOrderService.generateOrder(sysUser.getId());
        return Result.ok("success", orderId);
    }

    @GetMapping("/refreshAll")
    public Result<String> refreshAll() {
        JwtUtil.verifierFromContext();
        afdOrderService.updateData();
        afdOrderService.updateUser();
        return Result.ok("ok");
    }

    @GetMapping("/refreshAfdOrder")
    public Result<String> refreshAfdOrder() {
        JwtUtil.verifierFromContext();
        afdOrderService.updateData();
        return Result.ok("ok");
    }

    @GetMapping("/refreshAfdUser")
    public Result<String> refreshAfdUser() {
        JwtUtil.verifierFromContext();
        afdOrderService.updateUser();
        return Result.ok("ok");
    }

}
