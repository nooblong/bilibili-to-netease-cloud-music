package github.nooblong.btncm.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.btncm.bilibili.BilibiliClient;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.bilibili.UploadFailException;
import github.nooblong.btncm.bilibili.BilibiliFullVideo;
import github.nooblong.btncm.entity.SysUser;
import github.nooblong.btncm.entity.Subscribe;
import github.nooblong.btncm.entity.UploadDetail;
import github.nooblong.btncm.enums.UploadStatusTypeEnum;
import github.nooblong.btncm.netmusic.NetMusicClient;
import github.nooblong.btncm.service.FfmpegService;
import github.nooblong.btncm.service.SubscribeService;
import github.nooblong.btncm.service.UploadDetailService;
import github.nooblong.btncm.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * 上传视频到网易云的全部步骤
 */
@Scope("prototype")
@Component
@Slf4j
public class UploadJob {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final FfmpegService ffmpegService;
    final SubscribeService subscribeService;
    final UploadDetailService uploadDetailService;

    public UploadJob(BilibiliClient bilibiliClient,
                     NetMusicClient netMusicClient,
                     FfmpegService ffmpegService,
                     SubscribeService subscribeService,
                     UploadDetailService uploadDetailService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.ffmpegService = ffmpegService;
        this.subscribeService = subscribeService;
        this.uploadDetailService = uploadDetailService;
    }

    public void start(Long uploadDetailId,
                      Map<String, String> bilibiliCookie) {
        try {
            TaskContextHolder.set(new TaskContext());
            TaskContext context = TaskContextHolder.get();
            UploadDetail uploadDetail = uploadDetailService.getById(uploadDetailId);
            uploadDetail.setUploadRetryTimes(uploadDetail.getUploadRetryTimes() + 1);
            uploadDetail.setUploadStatus(UploadStatusTypeEnum.PROCESSING);
            uploadDetailService.updateById(uploadDetail);
            context.uploadDetailId = uploadDetailId;
            context.sysUser = Db.getById(uploadDetail.getUserId(), SysUser.class);
            log.info("开始处理: {}", uploadDetail);
            checkUploadPerDay(context.sysUser);
            Assert.notNull(context.sysUser, "user不能为空");
            Db.update(Wrappers.lambdaUpdate(SysUser.class).eq(SysUser::getId, context.sysUser.getId())
                    .setSql("remaining = remaining - 1"));
            getData(uploadDetail.getBvid(), uploadDetail.getCid(),
                    uploadDetail.getUseVideoCover(), uploadDetail.getUserId(), bilibiliCookie);

            codecAudio(uploadDetail.getBeginSec(), uploadDetail.getEndSec(),
                    uploadDetail.getOffset(), uploadDetail.getBitrate());

            // 上传之前先设置名字
            Subscribe subscribe = subscribeService.getById(uploadDetail.getSubscribeId());
            String regName;
            if (subscribe == null) {
                regName = "{title}";
            } else {
                regName = subscribe.getRegName();
            }
            uploadDetail.setUploadName(handleUploadName(regName, uploadDetail, context.bilibiliFullVideo));

            String voiceId = uploadNetease(String.valueOf(uploadDetail.getVoiceListId()),
                    uploadDetail.getUserId(),
                    uploadDetail.getUploadName(), uploadDetail.getPrivacy());

            UploadDetail newUploadDetail = uploadDetailService.getById(context.uploadDetailId);
            newUploadDetail.setVoiceId(Long.valueOf(voiceId));
            newUploadDetail.setUploadStatus(UploadStatusTypeEnum.SUCCESS);
            String logsString = context.getAllLogsText();
            newUploadDetail.setLog(logsString);
            Db.updateById(newUploadDetail);
            log.info("上传完成");
        } catch (UploadFailException uploadFailException) {
            TaskContext context = TaskContextHolder.get();
            log.error(uploadFailException.getuploadStatusTypeEnum().getDesc());
            UploadDetail byId = Db.getById(uploadDetailId, UploadDetail.class);
            byId.setLog(context.getAllLogsText());
            byId.setUploadStatus(uploadFailException.getuploadStatusTypeEnum());
            Db.updateById(byId);
        } catch (Exception e) {
            TaskContext context = TaskContextHolder.get();
            log.error("总处理失败", e);
            UploadDetail byId = Db.getById(uploadDetailId, UploadDetail.class);
            byId.setLog(context.getAllLogsText());
            byId.setUploadStatus(UploadStatusTypeEnum.ERROR);
            if (byId.getUploadRetryTimes() > Constant.UPLOAD_MAX_RETRY_TIMES) {
                byId.setUploadStatus(UploadStatusTypeEnum.MAX_RETRY);
            }
            Db.updateById(byId);
        } finally {
            delete();
            TaskContextHolder.clear();
        }
    }

    private void checkUploadPerDay(SysUser sysUser) throws UploadFailException {
        if (sysUser.getRemaining() <= 0 && sysUser.expired()) {
            log.error("每日上传次数用完");
            throw new UploadFailException(UploadStatusTypeEnum.OVER_UPLOAD_DAY);
        }
    }

    private void getData(String bvid, String cid, Long useVideoCover, Long userId,
                         Map<String, String> availableBilibiliCookie) throws Exception {
        TaskContext context = TaskContextHolder.get();
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(bvid);
        if (StrUtil.isNotEmpty(cid)) {
            simpleVideoInfo.setCid(cid);
        }
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(simpleVideoInfo, availableBilibiliCookie);
        if (bilibiliFullVideo.getVideoInfo().get("data").has("is_upower_exclusive")) {
            if (bilibiliFullVideo.getVideoInfo().get("data").get("is_upower_exclusive").asBoolean()) {
                throw new UploadFailException(UploadStatusTypeEnum.RECHARGE_VIDEO);
            }
        }
        context.bilibiliFullVideo = bilibiliFullVideo;
        context.musicPath = bilibiliClient.downloadFile(bilibiliFullVideo, availableBilibiliCookie, context.sysUser);

        if (useVideoCover == 1) {
            try {
                Path imagePath = bilibiliClient.downloadCover(bilibiliFullVideo);
                context.netImageId = transImage(imagePath, netMusicClient, userId);
            } catch (Exception e) {
                log.error("下载封面出错，跳过", e);
            }
        }
    }

    private String transImage(Path path, NetMusicClient netMusicClient, Long userId) {
        HashMap<String, Object> params = new HashMap<>();
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            int width = image.getWidth();
            int height = image.getHeight();
            int size;
            if (width < height) {
                size = width * width;
                params.put("imgY", String.valueOf((height - width) / 2));
            } else {
                size = height * height;
                params.put("imgX", String.valueOf((width - height) / 2));
            }
            params.put("size", String.valueOf(size));
        } catch (IOException e) {
            throw new RuntimeException("下载的封面图片读取失败: " + path);
        }
        params.put("imagePath", path.toString());
        JsonNode imageUploadAlloc = netMusicClient.getMusicDataByUserId(params, "imageUploadAlloc", userId);
        String docId = imageUploadAlloc.get("result").get("docId").asText();
        String objectKey = imageUploadAlloc.get("result").get("objectKey").asText();
        String token = imageUploadAlloc.get("result").get("token").asText();
        params.put("docId", docId);
        params.put("objectKey", objectKey);
        params.put("token", token);
        netMusicClient.getMusicDataByUserId(params, "imageUploadFirst", userId);
        JsonNode imageUploadSecond = netMusicClient.getMusicDataByUserId(params, "imageUploadSecond", userId);
        return imageUploadSecond.get("id").asText();

    }

    private void codecAudio(double beginSec, double endSec, double voiceOffset, int bitrate) {
        TaskContext context = TaskContextHolder.get();
        Path targetPath = ffmpegService.encodeMp3(context.musicPath, beginSec, endSec, voiceOffset, bitrate);
        context.musicPath = targetPath;
        try {
            log.info("转码后的文件大小: {}K", NumberUtil.round((double) Files.size(targetPath) / 1024, 2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String uploadNetease(String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
        TaskContext context = TaskContextHolder.get();
        String toAddDesc = "";
        toAddDesc += ("\n视频bvid: " + context.bilibiliFullVideo.getBvid());
        toAddDesc += ("\nb站作者: " + context.bilibiliFullVideo.getAuthor());
        toAddDesc += ("\n一键上传工具: github.com/nooblong/bilibili-to-netease-cloud-music");
        context.desc += toAddDesc;

        Assert.notNull(uploadName, "上传名字为空");
        if (uploadName.length() > 40) {
            uploadName = uploadName.substring(0, 40);
        }

        JsonNode voiceListDetail = netMusicClient.getVoiceListDetailByUserId(voiceListId, uploadUserId);
        String categoryId = voiceListDetail.get("categoryId").asText();
        String secondCategoryId = voiceListDetail.get("secondCategoryId").asText();
        String coverImgId = voiceListDetail.get("coverImgId").asText();
        String netImageId = StrUtil.isNotBlank(context.netImageId) ? context.netImageId : coverImgId;
        String voiceId = doUpload(netMusicClient, "mp3", uploadName, context.musicPath, voiceListId, netImageId,
                categoryId, secondCategoryId, context.desc, uploadUserId,
                Boolean.toString(privacy == 1), context.uploadDetailId);
        Assert.notNull(voiceId, "返回的声音id为空");
        return voiceId;
    }

    private String doUpload(NetMusicClient netMusicClient, String ext, String uploadName, Path path,
                            String voiceListId, String coverImgId, String categoryId,
                            String secondCategoryId, String description, Long userId, String privacy,
                            Long uploadDetailId) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("ext", ext);
        queryMap.put("fileName", UUID.randomUUID().toString().substring(0, 10) + uploadName);
        JsonNode audioUploadAlloc = netMusicClient.getMusicDataByUserId(queryMap, "audioUploadAlloc", userId);

        String docId = audioUploadAlloc.get("result").get("docId").asText();
        String objectKey = audioUploadAlloc.get("result").get("objectKey").asText();
        String token = audioUploadAlloc.get("result").get("token").asText();
        queryMap.put("docId", docId);
        queryMap.put("objectKey", objectKey);
        queryMap.put("token", token);

        JsonNode audioUploadFirst = netMusicClient.getMusicDataByUserId(queryMap, "audioUploadFirst", userId);
        String uploadId = audioUploadFirst.get("uploadId").asText();
        queryMap.put("uploadId", uploadId);

        try {
            queryMap.put("dataInputStream", new FileInputStream(path.toFile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        queryMap.put("uploadDetailId", uploadDetailId);

        JsonNode audioUploadSecond = netMusicClient.getMusicDataByUserId(queryMap, "audioUploadSecond", userId);

        queryMap.put("name", uploadName);
        queryMap.put("uploadResult", audioUploadSecond);
        queryMap.put("voiceListId", voiceListId);
        queryMap.put("coverImgId", coverImgId);
        queryMap.put("categoryId", categoryId);
        queryMap.put("secondCategoryId", secondCategoryId);
        queryMap.put("privacy", privacy);
        queryMap.put("description", description);

        JsonNode audioUploadThird = netMusicClient.getMusicDataByUserId(queryMap, "audioUploadThird", userId);

        JsonNode audioPreCheck = netMusicClient.getMusicDataByUserId(queryMap, "audioPreCheck", userId);
        Assert.isTrue(audioPreCheck.get("code").asInt() == 200, "声音发布失败: " + audioPreCheck.get("message").asText());

        JsonNode audioUpload = netMusicClient.getMusicDataByUserId(queryMap, "audioSubmit", userId);
        Assert.isTrue(audioUpload.get("code").asInt() == 200, "声音发布失败: " + audioPreCheck.get("message").asText());
        ArrayNode result = (ArrayNode) audioUpload.get("data");
        return result.get(0).asText();
    }

    private void delete() {
        Path path = Paths.get(Constant.TMP_FOLDER);
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(file -> {
                        if (!file.equals(path.toFile())) {
                            boolean delete = file.delete();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String handleUploadName(String regName, UploadDetail uploadDetail, BilibiliFullVideo fullVideo) {
        if (StrUtil.isNotBlank(uploadDetail.getUploadName())) {
            return uploadDetail.getUploadName();
        }
        if (uploadDetail.getSubscribeId() != null && uploadDetail.getSubscribeId() != 0) {
            if (StrUtil.isNotBlank(regName)) {
                // 匹配正则
                String r1 = "\\{\\{(.*?)\\}\\}";
                List<String> all = ReUtil.findAll(r1, regName, 1);
                for (String s : all) {
                    try {
                        String s1 = ReUtil.extractMulti(s, fullVideo.getTitle(), "$1");
                        regName = regName.replaceAll(ReUtil.escape("{{" + s + "}}"),
                                Objects.requireNonNullElse(s1, "无匹配结果"));
                    } catch (Exception e) {
                        regName = regName.replaceAll(ReUtil.escape("{{" + s + "}}"), "正则有误");
                    }
                }

                regName = regName.replaceAll("\\{pubdate}", DateUtil.format(fullVideo.getVideoCreateTime(), "yyyy.MM.dd"));
                regName = regName.replaceAll("\\{title}", fullVideo.getTitle());
                regName = regName.replaceAll("\\{partname}", fullVideo.getPartName());
                return regName;
            }
        }
        if (fullVideo.getHasMultiPart()) {
            return fullVideo.getPartName() + "-" + fullVideo.getTitle();
        } else {
            return fullVideo.getTitle();
        }
    }

}
