package github.nooblong.download.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.entity.SysInfo;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys")
public class SystemController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;

    public SystemController(BilibiliClient bilibiliClient,
                            NetMusicClient netMusicClient,
                            UploadDetailService uploadDetailService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
    }

    @GetMapping("/sysInfo")
    public Result<SysInfo> sysInfo() {
        SysInfo sysInfo = new SysInfo();
        try {
            sysInfo.setBilibiliCookieStatus(bilibiliClient.getAvailableBilibiliCookie() != null);
        } catch (RuntimeException e) {
            sysInfo.setBilibiliCookieStatus(false);
        }
        try {
            SysUser sysUser = JwtUtil.verifierFromContext();
            boolean b = netMusicClient.checkLogin(sysUser.getId());
            sysInfo.setNetCookieStatus(b);
        } catch (Exception e) {
            sysInfo.setNetCookieStatus(false);
        }
        return Result.ok("ok", sysInfo);
    }

    @GetMapping("/queueInfo")
    public Result<IPage<UploadDetail>> queueInfo(@RequestParam(name = "pageNo") int pageNo,
                                                 @RequestParam(name = "pageSize") int pageSize) {
        LambdaQueryWrapper<UploadDetail> queryWrapper = Wrappers.lambdaQuery(UploadDetail.class)
                .orderByDesc(UploadDetail::getPriority);
        IPage<UploadDetail> page = uploadDetailService.page(new Page<>(pageNo, pageSize), queryWrapper);
        return Result.ok("ok", page);
    }

}
