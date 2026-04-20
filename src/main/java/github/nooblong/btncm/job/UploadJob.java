package github.nooblong.btncm.job;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadJob {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final FfmpegService ffmpegService;
    final SubscribeService subscribeService;
    final UploadDetailService uploadDetailService;
    public static StringBuilder uploadLog = new StringBuilder();
    public static ListAppender<ILoggingEvent> listAppender;

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

    public static void redirectLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    public static void stopRedirectLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(UploadJob.class);
        List<ILoggingEvent> logsList = listAppender.list;
        String logs = logsList.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        uploadLog.append(logs);
        listAppender.stop();
        logger.detachAppender(listAppender);
        listAppender = null;
    }

    public void uploadOne() {
        UploadDetail upload = uploadDetailService.getToUploadWithCookie();
        if (upload == null) {
            return;
        }
        log.info("处理: {}", upload.getTitle());
        Map<String, String> availableBilibiliCookie;
        try {
            availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        } catch (RuntimeException e) {
            log.error("准备下载失败:", e);
            return;
        }
        this.process(upload.getId(), availableBilibiliCookie);
    }

    public static class Context {
        public Path musicPath;
        public String desc = "";
        public String netImageId;
        public BilibiliFullVideo bilibiliFullVideo;
        public Long uploadDetailId;
        public SysUser sysUser;
    }

    public void process(Long uploadDetailId, Map<String, String> availableBilibiliCookie) {
        redirectLog();
        UploadDetail uploadDetail = uploadDetailService.getById(uploadDetailId);
        uploadDetail.setUploadRetryTimes(uploadDetail.getUploadRetryTimes() + 1);
        uploadDetail.setUploadStatus(UploadStatusTypeEnum.PROCESSING);
        uploadDetailService.updateById(uploadDetail);
        Context context = new Context();
        context.uploadDetailId = uploadDetailId;
        context.sysUser = Db.getById(uploadDetail.getUserId(), SysUser.class);
        try {
            log.info("开始处理: {}", uploadDetail);
            checkUploadPerDay(context.sysUser);
            Assert.notNull(context.sysUser, "user不能为空");
            Db.update(Wrappers.lambdaUpdate(SysUser.class).eq(SysUser::getId, context.sysUser.getId())
                    .setSql("remaining = remaining - 1"));
            getData(context, uploadDetail.getBvid(), uploadDetail.getCid(),
                    uploadDetail.getUseVideoCover(), uploadDetail.getUserId(), availableBilibiliCookie);

            codecAudio(context, uploadDetail.getBeginSec(), uploadDetail.getEndSec(),
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

            String voiceId = uploadNetease(context, String.valueOf(uploadDetail.getVoiceListId()),
                    uploadDetail.getUserId(),
                    uploadDetail.getUploadName(), uploadDetail.getPrivacy());

            log.info("上传完成");
            clear(context, Long.valueOf(voiceId));
        } catch (UploadFailException uploadFailException) {
            log.error(uploadFailException.getuploadStatusTypeEnum().getDesc());
            stopRedirectLog();
            String uploadLogString = uploadLog.toString();
            uploadLog.setLength(0);
            UploadDetail byId = Db.getById(uploadDetailId, UploadDetail.class);
            byId.setLog(uploadLogString);
            byId.setUploadStatus(uploadFailException.getuploadStatusTypeEnum());
            Db.updateById(byId);
            delete();
        } catch (Exception e) {
            log.error("处理失败1, {}", e.getMessage());
            stopRedirectLog();
            String uploadLogString = uploadLog.toString();
            uploadLog.setLength(0);
            UploadDetail byId = Db.getById(uploadDetailId, UploadDetail.class);
            byId.setLog(uploadLogString);
            byId.setUploadStatus(UploadStatusTypeEnum.ERROR);
            if (byId.getUploadRetryTimes() > Constant.UPLOAD_MAX_RETRY_TIMES) {
                byId.setUploadStatus(UploadStatusTypeEnum.MAX_RETRY);
            }
            Db.updateById(byId);
            delete();
        }
    }

    private void checkUploadPerDay(SysUser sysUser) throws UploadFailException {
        if (sysUser.getRemaining() <= 0 && sysUser.expired()) {
            log.error("每日上传次数用完，请升级ssssssssvip");
            throw new UploadFailException(UploadStatusTypeEnum.OVER_UPLOAD_DAY);
        }
    }

    private void getData(Context context, String bvid, String cid, Long useVideoCover, Long userId,
                         Map<String, String> availableBilibiliCookie) throws Exception {
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
                context.netImageId = transImage(context, imagePath, netMusicClient, userId);
            } catch (Exception e) {
                log.error("下载封面出错，跳过");
            }
        }
    }

    private String transImage(Context context, Path path, NetMusicClient netMusicClient, Long userId) {
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
        JsonNode imageuploadalloc = netMusicClient.getMusicDataByUserId(params, "imageuploadalloc", userId);
        String docId = imageuploadalloc.get("result").get("docId").asText();
        String objectKey = imageuploadalloc.get("result").get("objectKey").asText();
        String token = imageuploadalloc.get("result").get("token").asText();
        params.put("docId", docId);
        params.put("objectKey", objectKey);
        params.put("token", token);
        netMusicClient.getMusicDataByUserId(params, "imageuploadfirst", userId);
        JsonNode imageuploadsecond = netMusicClient.getMusicDataByUserId(params, "imageuploadsecond", userId);
        return imageuploadsecond.get("id").asText();

    }

    private void codecAudio(Context context, double beginSec, double endSec, double voiceOffset, int bitrate) {
        Path targetPath = ffmpegService.encodeMp3(context.musicPath, beginSec, endSec, voiceOffset, bitrate);
        context.musicPath = targetPath;
        try {
            log.info("转码后的文件大小: {}K", NumberUtil.round((double) Files.size(targetPath) / 1024, 2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String uploadNetease(Context context, String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
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
        JsonNode audiouploadalloc = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadalloc", userId);

        String docId = audiouploadalloc.get("result").get("docId").asText();
        String objectKey = audiouploadalloc.get("result").get("objectKey").asText();
        String token = audiouploadalloc.get("result").get("token").asText();
        queryMap.put("docId", docId);
        queryMap.put("objectKey", objectKey);
        queryMap.put("token", token);

        JsonNode audiouploadfirst = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadfirst", userId);
        String uploadId = audiouploadfirst.get("uploadId").asText();
        queryMap.put("uploadId", uploadId);

        try {
            queryMap.put("dataInputStream", new FileInputStream(path.toFile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        queryMap.put("uploadDetailId", uploadDetailId);

        JsonNode audiouploadsecond = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadsecond", userId);

        queryMap.put("name", uploadName);
        queryMap.put("uploadResult", audiouploadsecond);
        queryMap.put("voiceListId", voiceListId);
        queryMap.put("coverImgId", coverImgId);
        queryMap.put("categoryId", categoryId);
        queryMap.put("secondCategoryId", secondCategoryId);
        queryMap.put("privacy", privacy);
        queryMap.put("description", description);

        JsonNode audiouploadthird = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadthird", userId);

        JsonNode audioprecheck = netMusicClient.getMusicDataByUserId(queryMap, "audioprecheck", userId);
        Assert.isTrue(audioprecheck.get("code").asInt() == 200, "声音发布失败: " + audioprecheck.get("message").asText());

        JsonNode audioupload = netMusicClient.getMusicDataByUserId(queryMap, "audiosubmit", userId);
        Assert.isTrue(audioupload.get("code").asInt() == 200, "声音发布失败: " + audioprecheck.get("message").asText());
        ArrayNode result = (ArrayNode) audioupload.get("data");
        return result.get(0).asText();
    }

    private void clear(Context context, Long voiceId) {
        UploadDetail newUploadDetail = uploadDetailService.getById(context.uploadDetailId);
        newUploadDetail.setVoiceId(voiceId);
        newUploadDetail.setUploadStatus(UploadStatusTypeEnum.SUCCESS);
        stopRedirectLog();
        String uploadLogString = uploadLog.toString();
        newUploadDetail.setLog(uploadLogString);
        Db.updateById(newUploadDetail);
        uploadLog.setLength(0);
        // 清理数据
        delete();
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
