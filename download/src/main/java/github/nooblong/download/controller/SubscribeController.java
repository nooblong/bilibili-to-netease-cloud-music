package github.nooblong.download.controller;

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
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.enums.SubscribeTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.SubscribeReg;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SubscribeController {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final UploadDetailService uploadDetailService;

    public SubscribeController(BilibiliClient bilibiliClient,
                               NetMusicClient netMusicClient,
                               UploadDetailService uploadDetailService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.uploadDetailService = uploadDetailService;
    }

    @PutMapping("/subscribe/{id}")
    public Result<Subscribe> edit(@RequestBody Subscribe subscribe, @PathVariable(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        if (!uploadDetailService.hasUploaded(user.getId())) {
            return Result.fail("请先上传一遍单个的证明可以上传");
        }
        Subscribe byId = Db.getById(id, Subscribe.class);
        if (subscribe.getCrack() > 0) {
            Long userId = JwtUtil.verifierFromContext().getId();
            if (!userId.equals(1L)) {
                return Result.fail("暂不开放绕过订阅");
            }
        }
        Assert.isTrue(byId.getUserId().equals(user.getId()), "你想干嘛?");
        if (subscribe.getType() == SubscribeTypeEnum.PART) {
            // 解析成bvid而不是url
            subscribe.setProcessTime(null);
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid(subscribe.getTargetId());
            BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
            JsonNode info = bilibiliFullVideo.getVideoInfo();
            subscribe.setTargetId(info.get("data").get("bvid").asText());
        }
        if (!subscribe.getVoiceListId().equals(byId.getVoiceListId())) {
            // 获取播客图片
            JsonNode voiceListDetail = netMusicClient.getVoiceListDetail(subscribe.getVoiceListId().toString(), user.getId());
            String text = voiceListDetail.get("coverUrl").asText();
            subscribe.setNetCover(text);
        }

        Db.remove(Wrappers.lambdaQuery(SubscribeReg.class).eq(SubscribeReg::getSubscribeId, byId.getId()));
        List<SubscribeReg> subscribeRegs = subscribe.getSubscribeRegs();
        subscribeRegs.forEach(subscribeReg -> subscribeReg.setSubscribeId(byId.getId()));
        Db.saveBatch(subscribeRegs);

        Db.updateById(subscribe);
        return Result.ok("ok", subscribe);
    }

    @PostMapping("/subscribe")
    public Result<Subscribe> add(@RequestBody Subscribe subscribe) {
        SysUser user = JwtUtil.verifierFromContext();
        subscribe.setUserId(user.getId());
        if (subscribe.getCrack() > 0) {
            Long id = JwtUtil.verifierFromContext().getId();
            if (!id.equals(1L)) {
                return Result.fail("暂不开放绕过订阅");
            }
        }
        if (subscribe.getType() == SubscribeTypeEnum.PART) {
            // 解析成bvid而不是url
            subscribe.setProcessTime(null);
            SimpleVideoInfo simpleVideoInfo = new SimpleVideoInfo().setBvid(subscribe.getTargetId());
            BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
            JsonNode info = bilibiliFullVideo.getVideoInfo();
            subscribe.setTargetId(info.get("bvid").asText());
        }
        // 获取播客图片
        JsonNode voiceListDetail = netMusicClient.getVoiceListDetail(subscribe.getVoiceListId().toString(), user.getId());
        String text = voiceListDetail.get("coverUrl").asText();
        subscribe.setNetCover(text);
        Db.save(subscribe);
        subscribe.getSubscribeRegs().forEach(subscribeReg -> subscribeReg.setSubscribeId(subscribe.getId()));
        Db.saveBatch(subscribe.getSubscribeRegs());
        return Result.ok("ok", subscribe);
    }

    @DeleteMapping("/subscribe/{id}")
    public Result<Subscribe> delete(@PathVariable(name = "id") Long id) {
        SysUser user = JwtUtil.verifierFromContext();
        Subscribe byId = Db.getById(id, Subscribe.class);
        Assert.isTrue(byId.getUserId().equals(user.getId()), "你想干嘛?");
        Db.removeById(byId);
        Db.remove(Wrappers.lambdaQuery(SubscribeReg.class).eq(SubscribeReg::getSubscribeId, byId.getId()));
        return Result.ok("ok", byId);
    }

    @GetMapping("/subscribe")
    public Result<IPage<Subscribe>> subscribeList(@RequestParam(name = "pageNo") int pageNo,
                                                 @RequestParam(name = "pageSize") int pageSize,
                                                 HttpServletResponse response) {
        IPage<Subscribe> list = Db.page(new Page<>(pageNo, pageSize) ,Subscribe.class);
        LambdaQueryWrapper<SubscribeReg> regLambdaQueryWrapper = Wrappers.lambdaQuery(SubscribeReg.class)
                .in(SubscribeReg::getSubscribeId, list.getRecords().stream().map(Subscribe::getId).collect(Collectors.toList()));
        List<SubscribeReg> subscribeRegs = Db.list(regLambdaQueryWrapper);
        list.getRecords().forEach(subscribe -> subscribeRegs.forEach(subscribeReg -> {
            if (subscribeReg.getSubscribeId().equals(subscribe.getId())) {
                subscribe.getSubscribeRegs().add(subscribeReg);
            }
            subscribe.setTypeDesc(subscribe.getType().getDesc());
        }));
        response.addHeader("Content-Range", String.valueOf(Db.count(Subscribe.class)));
        return Result.ok("ok", list);
    }

    @GetMapping("/subscribe/detail")
    public Result<Subscribe> subscribeList(@RequestParam(name = "id") Long id) {
        Subscribe byId = Db.getById(id, Subscribe.class);
        LambdaQueryWrapper<SubscribeReg> regLambdaQueryWrapper = Wrappers.lambdaQuery(SubscribeReg.class)
                .eq(SubscribeReg::getSubscribeId, byId.getId());
        List<SubscribeReg> subscribeRegs = Db.list(regLambdaQueryWrapper);
        byId.setSubscribeRegs(subscribeRegs);
        return Result.ok("ok", byId);
    }

    @GetMapping("/subscribe/subscribeVoiceList")
    public Result<ArrayNode> subscribeVoiceList() {
        List<Subscribe> list = Db.list(Subscribe.class);
        if (list.isEmpty()) {
            return Result.fail("订阅为空");
        }
        Map<Long, List<Subscribe>> longListMap = SimpleQuery.listGroupBy(list, Subscribe::getVoiceListId);
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode resultArray = objectMapper.createArrayNode();
        longListMap.forEach((voiceListId, subscribes) -> {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("voiceListId", voiceListId);
            objectNode.putPOJO("subscribes", subscribes);
            resultArray.add(objectNode);
        });
        return Result.ok("ok", resultArray);
    }


}
