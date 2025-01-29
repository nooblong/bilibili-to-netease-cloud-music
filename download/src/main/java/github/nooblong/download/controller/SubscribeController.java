package github.nooblong.download.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.bilibili.enums.SubscribeTypeEnum;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.SubscribeReg;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscribe")
public class SubscribeController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;
    final SubscribeService subscribeService;

    public SubscribeController(BilibiliClient bilibiliClient,
                               NetMusicClient netMusicClient,
                               UploadDetailService uploadDetailService,
                               SubscribeService subscribeService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
        this.subscribeService = subscribeService;
    }

    @PostMapping("/edit")
    public Result<Subscribe> edit(@RequestBody Subscribe subscribe, @RequestParam(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(id, Subscribe.class);
        if (subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.remove(Wrappers.lambdaQuery(SubscribeReg.class).eq(SubscribeReg::getSubscribeId, byId.getId()));
        List<SubscribeReg> subscribeRegs = subscribe.getSubscribeRegs();
        subscribeRegs.forEach(subscribeReg -> subscribeReg.setSubscribeId(byId.getId()));
        Db.saveBatch(subscribeRegs);
        Db.updateById(subscribe);
        return Result.ok("ok", subscribe);
    }

    @PostMapping("/add")
    public Result<Subscribe> add(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        subscribe.setUserId(user.getId());
        if (subscribe.getCrack() > 0) {
            if (user.getIsAdmin() != 1) {
                return Result.fail("fail: crack");
            }
        }
        // 获取播客图片
        JsonNode voiceListDetail = netMusicClient.getVoiceListDetail(subscribe.getVoiceListId().toString(),
                user.getId());
        String text = voiceListDetail.get("coverUrl").asText();
        subscribe.setNetCover(text);
        Db.save(subscribe);
        subscribe.getSubscribeRegs().forEach(subscribeReg -> subscribeReg.setSubscribeId(subscribe.getId()));
        Db.saveBatch(subscribe.getSubscribeRegs());
        return Result.ok("ok", subscribe);
    }

    @DeleteMapping("/delete")
    public Result<Subscribe> delete(@RequestParam(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(id, Subscribe.class);
        Assert.isTrue(byId.getUserId().equals(user.getId()), "fail: not owner");
        Db.removeById(byId);
        Db.remove(Wrappers.lambdaQuery(SubscribeReg.class).eq(SubscribeReg::getSubscribeId, byId.getId()));
        return Result.ok("ok", byId);
    }

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
        LambdaQueryWrapper<SubscribeReg> regLambdaQueryWrapper = Wrappers.lambdaQuery(SubscribeReg.class)
                .in(SubscribeReg::getSubscribeId,
                        list.stream().map(Subscribe::getId).collect(Collectors.toList()));
        List<SubscribeReg> subscribeRegs = Db.list(regLambdaQueryWrapper);
        List<SysUser> users = Db.list(SysUser.class);
        Map<Long, SysUser> longSysUserMap = SimpleQuery.list2Map(users, SysUser::getId, i -> i);
        list.forEach(subscribe -> {
            subscribeRegs.forEach(subscribeReg -> {
                if (subscribeReg.getSubscribeId().equals(subscribe.getId())) {
                    subscribe.getSubscribeRegs().add(subscribeReg);
                }
            });
            subscribe.setTypeDesc(subscribe.getType().getDesc());
            subscribe.setUserName(longSysUserMap.get(subscribe.getUserId()).getUsername());
            subscribe.setLog(CommonUtil.limitString(subscribe.getLog(), 10));
        });
        response.addHeader("Content-Range", String.valueOf(Db.count(Subscribe.class)));
        return Result.ok("ok", list);
    }

    @GetMapping("/detail")
    public Result<Subscribe> subscribeList(@RequestParam(name = "id") Long id) {
        Subscribe byId = Db.getById(id, Subscribe.class);
        LambdaQueryWrapper<SubscribeReg> regLambdaQueryWrapper = Wrappers.lambdaQuery(SubscribeReg.class)
                .eq(SubscribeReg::getSubscribeId, byId.getId());
        List<SubscribeReg> subscribeRegs = Db.list(regLambdaQueryWrapper);
        byId.setSubscribeRegs(subscribeRegs);
        return Result.ok("ok", byId);
    }

    @GetMapping("/checkUpJob")
    public Result<String> checkUpJob() {
        Assert.isTrue(JwtUtil.verifierFromContext().getIsAdmin() == 1, "fail: no permission");
        subscribeService.checkAndSave();
        return Result.ok("ok");
    }

}
