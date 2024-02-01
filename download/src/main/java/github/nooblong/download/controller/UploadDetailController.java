package github.nooblong.download.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.model.Result;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.api.AddQueueRequest;
import github.nooblong.download.api.AddToMyRequest;
import github.nooblong.download.api.DataResponse;
import github.nooblong.download.api.RecentResponse;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.mq.MessageSender;
import github.nooblong.download.mq.VideoMessage;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.utils.Constant;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UploadDetailController {
    private final UploadDetailService uploadDetailService;
    final OkHttpClient okHttpClient;
    final NetMusicClient netMusicClient;
    final BilibiliUtil bilibiliUtil;
    final MessageSender messageSender;

    public UploadDetailController(UploadDetailService uploadDetailService,
                                  OkHttpClient okHttpClient,
                                  NetMusicClient netMusicClient,
                                  BilibiliUtil bilibiliUtil,
                                  MessageSender messageSender) {
        this.uploadDetailService = uploadDetailService;
        this.okHttpClient = okHttpClient;
        this.messageSender = messageSender;
        this.netMusicClient = netMusicClient;
        this.bilibiliUtil = bilibiliUtil;
    }

    @GetMapping("/uploadDetail/{id}")
    public Result<UploadDetail> get(@PathVariable(name = "id") Long id) {
        return Result.ok("ok", Db.getById(id, UploadDetail.class));
    }

    @PostMapping("/download/addQueue")
    public Result<String> addQueue(@RequestBody @Validated AddQueueRequest req) {
        Long userId = JwtUtil.verifierFromContext().getId();

        BilibiliVideo bilibiliVideo = bilibiliUtil.createByUrl(req.getBvid());
        // 查重
        boolean unique = uploadDetailService.isUnique(bilibiliVideo.getBvid(),
                req.getCid() == null ? "" : req.getCid(),
                req.getVoiceListId());
        if (!unique) {
            return Result.fail("查重率100%!");
        }

        UploadDetail uploadDetail = new UploadDetail();
        uploadDetail.setBvid(bilibiliVideo.getBvid());
        uploadDetail.setCid(req.getCid());
        uploadDetail.setVoiceListId(req.getVoiceListId());
        uploadDetail.setUseVideoCover(req.isUseDefaultImg() ? 1L : 0L);
        uploadDetail.setVideoBeginSec(req.getVoiceBeginSec());
        uploadDetail.setVideoEndSec(req.getVoiceEndSec());
        uploadDetail.setVoiceOffset(req.getVoiceOffset());
        uploadDetail.setUploadName(req.getCustomUploadName());
        uploadDetail.setUserId(userId);

        if (req.isCrack()) {
            if (!userId.equals(Constant.adminUserId) && userId >= 10000) {
                return Result.fail("暂不开放");
            } else {
                uploadDetail.setCrack(1L);
            }
        }

        Db.save(uploadDetail);
        
        messageSender.sendUploadDetailId(uploadDetail.getId());
        return Result.ok("添加队列成功");
    }

    @PostMapping("/download/addToMyList")
    public Result<String> addToMyList(@RequestBody @Validated AddToMyRequest req, HttpServletRequest request) throws JsonProcessingException {
        // todo: 没做完
        String token = request.getHeader("Access-Token");
        UploadDetail uploadDetail = Db.getById(req.getVoiceDetailId(), UploadDetail.class);
        SysUser user = JwtUtil.verifierToken(token);
        VideoMessage videoMessage = new VideoMessage().setUrl(uploadDetail.getBvid())
                .setUploadVoiceListId(Long.valueOf(req.getVoiceListId()))
                .setUploadUserId(user.getId());
        ObjectMapper objectMapper = new ObjectMapper();
        if (uploadDetail.getVideoInfo() != null &&
                objectMapper.readTree(uploadDetail.getVideoInfo()).has("cid")
                && !objectMapper.readTree(uploadDetail.getVideoInfo()).get("cid").equals(NullNode.getInstance())) {
            videoMessage.setCid(objectMapper.readTree(uploadDetail.getVideoInfo()).get("cid").asText());
        }
        return Result.ok("没做完");
    }

    @GetMapping("/data/info")
    public Result<DataResponse> getVideoInfo() {
        DataResponse dataResponse = new DataResponse();
        dataResponse.setRegisterNum(Db.count(SysUser.class));
        Path path = FileSystems.getDefault().getPath("/");
        long usableSpace;
        long totalSpace;
        try {
            FileStore fileStore = Files.getFileStore(path);
            usableSpace = fileStore.getUsableSpace();
            totalSpace = fileStore.getTotalSpace();

            System.out.println("Usable space: " + usableSpace + " mb");
            System.out.println("Total space: " + totalSpace + " bytes");
        } catch (IOException e) {
            return Result.fail("系统错误");
        }
        dataResponse.setAddQueueNum(Db.count(UploadDetail.class));
        dataResponse.setDiskUseNum((totalSpace - usableSpace) / 1024 / 1024);
        return Result.ok("获取成功", dataResponse);
    }

    @GetMapping("/data/recent")
    public Result<IPage<RecentResponse>> recent(@RequestParam(name = "pageNo") int pageNo,
                                                @RequestParam(name = "pageSize") int pageSize,
                                                @RequestParam(required = false, name = "title") String title,
                                                @RequestParam(required = false, name = "remark") String remark,
                                                @RequestParam(required = false, name = "username") String username,
                                                @RequestParam(required = false, name = "status")String status) {
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
            wrapper.notIn(UploadDetail::getDisplayStatus, "ONLINE", "ONLY_SELF_SEE", "AUDITING");
        } else {
            wrapper.like(status != null, UploadDetail::getDisplayStatus, status);
        }
        IPage<UploadDetail> page = uploadDetailService.page(pageNew, wrapper);

        List<RecentResponse> recentResponses = new ArrayList<>();
        for (UploadDetail record : page.getRecords()) {
            RecentResponse recentResponse = new RecentResponse();
            recentResponse.setUserName(longSysUserMap.get(record.getUserId()).getUsername());
            recentResponse.setId(String.valueOf(record.getId()));
            recentResponse.setDisplayStatus(record.getDisplayStatus());
            recentResponse.setCreateTime(DateUtil.formatDateTime(record.getCreateTime()));
            recentResponse.setName(record.getTitle());
            recentResponse.setVoiceId(record.getVoiceId().toString());
            recentResponse.setVoiceListId(record.getVoiceListId().toString());
            recentResponses.add(recentResponse);
        }
        Page<RecentResponse> result = new Page<RecentResponse>(page.getCurrent(), page.getSize(),
                page.getTotal()).setRecords(recentResponses);
        return Result.ok("查询成功", result);
    }

    @GetMapping("/data/voiceListSong")
    public Result<IPage<UploadDetail>> voiceListSong(@RequestParam(name = "pageNo") int pageNo,
                                                     @RequestParam(name = "pageSize") int pageSize,
                                                     @RequestParam(required = false, name = "search") String search,
                                                     @RequestParam(name = "voiceListId") String voiceListId,
                                                     String status) {
        IPage<UploadDetail> pageNew = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UploadDetail> wrapper = new LambdaQueryWrapper<UploadDetail>().orderByDesc(UploadDetail::getId);
        wrapper.like(search != null, UploadDetail::getUploadName, search);
        wrapper.eq(status != null, UploadDetail::getDisplayStatus, status);
        if (status != null && status.equals("other")) {
            wrapper.notIn(UploadDetail::getDisplayStatus, "ONLINE", "ONLY_SELF_SEE", "AUDITING");
        }
        wrapper.eq(UploadDetail::getVoiceListId, voiceListId);
        IPage<UploadDetail> page = uploadDetailService.page(pageNew, wrapper);
        page.getRecords().forEach(i -> i.setUserId(null));
        return Result.ok("查询成功", page);
    }

    @GetMapping("/uploadDetail/checkHasUploaded")
    public Result<Boolean> checkHasUploaded() {
        return Result.ok("ok", uploadDetailService.hasUploaded(JwtUtil.verifierFromContext().getId()));
    }

    @GetMapping("/uploadDetail/uploadAllOnlySelfSee")
    public Result<String> uploadAllOnlySelfSee(@RequestParam(name = "voiceListId") Long voiceListId) {
        Assert.isTrue(JwtUtil.verifierFromContext().getId().equals(1L), "???");
        uploadDetailService.uploadAllOnlySelfSee(voiceListId);
        return Result.ok("ok!");
    }

}