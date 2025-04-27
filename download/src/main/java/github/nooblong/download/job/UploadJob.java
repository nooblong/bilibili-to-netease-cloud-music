package github.nooblong.download.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.SubscribeTypeEnum;
import github.nooblong.download.UploadStatusTypeEnum;
import github.nooblong.download.VideoOrder;
import github.nooblong.download.bilibili.*;
import github.nooblong.download.bilibili.enums.UserVideoOrder;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadJob {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final FfmpegService ffmpegService;
    final SubscribeService subscribeService;
    final UploadDetailService uploadDetailService;
    final CacheManager cacheManager;

    public UploadJob(BilibiliClient bilibiliClient,
                     NetMusicClient netMusicClient,
                     FfmpegService ffmpegService,
                     SubscribeService subscribeService,
                     UploadDetailService uploadDetailService,
                     CacheManager cacheManager) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.ffmpegService = ffmpegService;
        this.subscribeService = subscribeService;
        this.uploadDetailService = uploadDetailService;
        this.cacheManager = cacheManager;
    }

    public void uploadOne() {
        LambdaQueryWrapper<UploadDetail> wrapper = Wrappers.lambdaQuery(UploadDetail.class)
                .eq(UploadDetail::getUploadStatus, UploadStatusTypeEnum.WAIT.name())
                .orderByDesc(UploadDetail::getPriority)
                .orderByAsc(UploadDetail::getCreateTime)
                .last("limit 1");
        List<UploadDetail> uploadDetailList = uploadDetailService.list(wrapper);
        if (uploadDetailList.isEmpty()) {
            log.debug("全部上传完毕");
            return;
        }
        log.info("处理: {}", uploadDetailList.get(0).getTitle());
        Map<String, String> availableBilibiliCookie;
        try {
            availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        } catch (RuntimeException e) {
            log.info("准备下载:{}: 没有可用b站cookie", uploadDetailList.get(0).getTitle());
            return;
        }
        log.info("开始下载:{}...", uploadDetailList.get(0).getTitle());
        this.process(uploadDetailList.get(0).getId(), availableBilibiliCookie);
    }

    public static class Context {
        public Path musicPath;
        public String desc = "";
        public String netImageId;
        public BilibiliFullVideo bilibiliFullVideo;
        public Long uploadDetailId;
    }

    public void process(Long uploadDetailId, Map<String, String> availableBilibiliCookie) {

        UploadDetail uploadDetail = uploadDetailService.getById(uploadDetailId);
        Assert.notNull(uploadDetail, "uploadDetail为空");

        // 先更新了信息先，其他不管
        uploadDetail.setUploadRetryTimes(uploadDetail.getUploadRetryTimes() + 1);
        uploadDetail.setUploadStatus(UploadStatusTypeEnum.PROCESSING);
        uploadDetailService.updateById(uploadDetail);

        Optional.ofNullable(cacheManager.getCache("sys/queueInfo")).ifPresent(Cache::clear);
        Optional.ofNullable(cacheManager.getCache("subscribe/list")).ifPresent(Cache::clear);
        Optional.ofNullable(cacheManager.getCache("uploadDetail/list")).ifPresent(Cache::clear);

        Context context = new Context();
        context.uploadDetailId = uploadDetailId;
        try {
            // 收集错误信息
            uploadDetailService.logNow(context.uploadDetailId, ">>> 开始下载音频");
            int retry = 0;
            while (retry < 3) {
                try {
                    getData(context, uploadDetail.getBvid(), uploadDetail.getCid(),
                            uploadDetail.getUseVideoCover() == 1, uploadDetail.getUserId(), availableBilibiliCookie);
                    break;
                } catch (Exception e) {
                    retry++;
                    log.error("下载音频失败, 重试: {}", retry + 1);
                    uploadDetailService.logNow(uploadDetailId, "下载音频失败, 重试: " + retry + 1);
                }
            }
            uploadDetailService.logNow(context.uploadDetailId, ">>> 下载音频成功");
            uploadDetailService.logNow(context.uploadDetailId, ">>> 下载封面成功");
            uploadDetailService.logNow(context.uploadDetailId, ">>> 开始转码");
            codecAudio(context, uploadDetail.getBeginSec(), uploadDetail.getEndSec(),
                    uploadDetail.getOffset(), uploadDetail.getBitrate());
            uploadDetailService.logNow(context.uploadDetailId, ">>> 转码成功");
            // 上传之前先设置名字
            uploadDetail.setUploadName(handleUploadName(context, uploadDetail));
            uploadDetailService.logNow(context.uploadDetailId, ">>> 开始上传网易云");
            String voiceId = uploadNetease(context, String.valueOf(uploadDetail.getVoiceListId()),
                    uploadDetail.getUserId(),
                    uploadDetail.getUploadName(), uploadDetail.getPrivacy());
            uploadDetailService.logNow(context.uploadDetailId, ">>> 上传网易云成功");
            uploadDetailService.logNow(context.uploadDetailId, ">>> 开始清除垃圾");
            clear(context, Long.valueOf(voiceId));
            uploadDetailService.logNow(context.uploadDetailId, ">>> 清除垃圾成功");

            uploadDetailService.logNow(context.uploadDetailId, ">>> 单曲上传成功, 声音id: " + voiceId);
            Optional.ofNullable(cacheManager.getCache("sys/queueInfo")).ifPresent(Cache::clear);
            Optional.ofNullable(cacheManager.getCache("subscribe/list")).ifPresent(Cache::clear);
            Optional.ofNullable(cacheManager.getCache("uploadDetail/list")).ifPresent(Cache::clear);
        } catch (Exception e) {
            uploadDetail.setUploadStatus(UploadStatusTypeEnum.ERROR);
            if (uploadDetail.getUploadRetryTimes() > Constant.UPLOAD_MAX_RETRY_TIMES) {
                uploadDetail.setUploadStatus(UploadStatusTypeEnum.MAX_RETRY);
            }
            Db.updateById(uploadDetail);
            uploadDetailService.logNow(context.uploadDetailId, ">>> 声音上传失败: " + CommonUtil.getExceptionStackTraceAsString(e));
            delete(context);
            uploadDetailService.logNow(context.uploadDetailId, ">>> 垃圾文件清理成功: " + e.getMessage());
            Optional.ofNullable(cacheManager.getCache("sys/queueInfo")).ifPresent(Cache::clear);
            Optional.ofNullable(cacheManager.getCache("subscribe/list")).ifPresent(Cache::clear);
            Optional.ofNullable(cacheManager.getCache("uploadDetail/list")).ifPresent(Cache::clear);
        }
    }

    private void getData(Context context, String bvid, String cid, boolean useVideoCover, Long userId,
                         Map<String, String> availableBilibiliCookie) throws Exception {
        assert bvid != null;
        assert !useVideoCover || userId != null && userId != 0;
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.getSimpleVideoInfoByBvidOrUrl(bvid);
        if (StrUtil.isNotEmpty(cid)) {
            simpleVideoInfo.setCid(cid);
        }
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(simpleVideoInfo, availableBilibiliCookie);
        context.bilibiliFullVideo = bilibiliFullVideo;
        context.musicPath = bilibiliClient.downloadFile(bilibiliFullVideo, availableBilibiliCookie, context);
        if (useVideoCover) {
            uploadDetailService.logNow(context.uploadDetailId, ">> 开始下载封面");
            Path imagePath = bilibiliClient.downloadCover(bilibiliFullVideo);
            uploadDetailService.logNow(context.uploadDetailId, ">>> 下载封面成功");
            context.netImageId = transImage(context, imagePath, netMusicClient, userId);
        } else {
            uploadDetailService.logNow(context.uploadDetailId, ">>> 跳过下载封面");
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
            uploadDetailService.logNow(context.uploadDetailId, ">>> 下载的封面图片读取失败: " + path);
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
        uploadDetailService.logNow(context.uploadDetailId, ">>> 裁剪封面成功" + imageuploadsecond.toString());
        return imageuploadsecond.get("id").asText();

    }

    private void codecAudio(Context context, double beginSec, double endSec, double voiceOffset, int bitrate) {
        Path targetPath = ffmpegService.encodeMp3(context.musicPath, beginSec, endSec, voiceOffset, bitrate);
        String ext = CommonUtil.getFileExt(context.musicPath.getFileName().toString());
        context.musicPath = targetPath;
        String s1 = "编码:" + ext;
        try {
            log.info("转码后的文件大小: {}K", NumberUtil.round((double) Files.size(targetPath) / 1024, 2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        uploadDetailService.logNow(context.uploadDetailId, ">>> 目标码率: " + bitrate);
        uploadDetailService.logNow(context.uploadDetailId, ">>> " + s1 + "音频转码成功");
    }

    private String uploadNetease(Context context, String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
        uploadDetailService.logNow(context.uploadDetailId, ">>> 开始上传网易云");
        String toAddDesc = "";
        toAddDesc += ("\n视频bvid: " + context.bilibiliFullVideo.getBvid());
        toAddDesc += ("\nb站作者: " + context.bilibiliFullVideo.getAuthor());
        toAddDesc += ("\n一键上传工具: github.com/nooblong/bilibili-to-netease-cloud-music");
        context.desc += toAddDesc;
        uploadDetailService.logNow(context.uploadDetailId, CommonUtil.limitString(">>> 添加介绍: " +
                toAddDesc.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\t", " ")));

        Assert.notNull(uploadName, "上传名字为空");
        if (uploadName.length() > 40) {
            uploadName = uploadName.substring(0, 40);
        }

        String ext = CommonUtil.getFileExt(context.musicPath.getFileName().toString());
        JsonNode voiceListDetail = netMusicClient.getVoiceListDetailByUserId(voiceListId, uploadUserId);
        String categoryId = voiceListDetail.get("categoryId").asText();
        String secondCategoryId = voiceListDetail.get("secondCategoryId").asText();
        String coverImgId = voiceListDetail.get("coverImgId").asText();
        String netImageId = StrUtil.isNotBlank(context.netImageId) ? context.netImageId : coverImgId;
        uploadDetailService.logNow(context.uploadDetailId, ">>> 获取播客信息: " + voiceListDetail +
                "\nvoiceListId: " + voiceListId + " imageId: " + netImageId + ", uploadName: " + uploadName
                + ", uploadUserId: " + uploadUserId);
//        return "666";
        String voiceId = doUpload(netMusicClient, ext, uploadName, context.musicPath, voiceListId, netImageId,
                categoryId, secondCategoryId, context.desc, uploadUserId,
                privacy == 1 ? "true" : "false", context.uploadDetailId);
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
        Db.updateById(newUploadDetail);
        uploadDetailService.logNow(context.uploadDetailId, ">>> 数据库数据已更新");
        // 清理数据
        delete(context);
    }

    private void delete(Context context) {
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
            uploadDetailService.logNow(context.uploadDetailId, ">>> 删除下载文件成功");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String handleUploadName(Context context, UploadDetail uploadDetail) {
        if (StrUtil.isNotBlank(uploadDetail.getUploadName())) {
            return uploadDetail.getUploadName();
        }
        if (uploadDetail.getSubscribeId() != null && uploadDetail.getSubscribeId() != 0) {
            Subscribe subscribe = subscribeService.getById(uploadDetail.getSubscribeId());
            String regName = subscribe.getRegName();
            if (StrUtil.isNotBlank(regName)) {
                String title = context.bilibiliFullVideo.getTitle();
                String partName = context.bilibiliFullVideo.getPartName() == null ?
                        "" : context.bilibiliFullVideo.getPartName();
                Date videoCreateTime = context.bilibiliFullVideo.getVideoCreateTime();

                // 匹配正则
                String r1 = "\\{\\{(.*?)\\}\\}";
                List<String> all = ReUtil.findAll(r1, regName, 1);
                for (String s : all) {
                    try {
                        String s1 = ReUtil.extractMulti(s, title, "$1");
                        regName = regName.replaceAll(ReUtil.escape("{{" + s + "}}"),
                                Objects.requireNonNullElse(s1, "无匹配结果"));
                    } catch (Exception e) {
                        regName = regName.replaceAll(ReUtil.escape("{{" + s + "}}"), "正则有误");
                    }
                }

                regName = regName.replaceAll("\\{pubdate}", DateUtil.format(videoCreateTime, "yyyy.MM.dd"));
                regName = regName.replaceAll("\\{title}", title);
                regName = regName.replaceAll("\\{partname}", partName);
                return regName;
            }
        }
        if (context.bilibiliFullVideo.getHasMultiPart()) {
            return context.bilibiliFullVideo.getPartName() + "-" + context.bilibiliFullVideo.getTitle();
        } else {
            return context.bilibiliFullVideo.getTitle();
        }
    }

    public List<String> test(Long subscribeId) {
        int times = 10;
        Subscribe subscribe = Db.getById(subscribeId, Subscribe.class);
        List<UploadDetail> uploadDetails = new ArrayList<>();
        List<String> result = new ArrayList<>();
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        if (subscribe.getType() == SubscribeTypeEnum.UP) {
            UpIterator upIterator = new UpIterator(bilibiliClient, subscribe.getUpId(), subscribe.getKeyWord(),
                    subscribe.getLimitSec(), subscribe.getMinSec(), VideoOrder.valueOf(subscribe.getVideoOrder()),
                    UserVideoOrder.PUBDATE, subscribe.getCheckPart() == 1,
                    availableBilibiliCookie, subscribe.getLastTotalIndex(), subscribe.getChannelIds(), new AtomicInteger(-1));
            uploadDetails = subscribeService.testProcess(subscribe, upIterator, times);
        }
        if (subscribe.getType() == SubscribeTypeEnum.FAVORITE) {
            String favIds = subscribe.getChannelIds();
            List<String> favIdList = CommonUtil.toList(favIds);
            for (String favId : favIdList) {
                FavoriteIterator favIterator = new FavoriteIterator(favId, bilibiliClient,
                        subscribe.getLimitSec(), subscribe.getMinSec(), subscribe.getCheckPart() == 1,
                        availableBilibiliCookie);
                List<UploadDetail> partDetails = subscribeService.testProcess(subscribe, favIterator, times);
                uploadDetails.addAll(partDetails);
            }
        }
        if (!uploadDetails.isEmpty()) {
            Map<String, String> cookie = bilibiliClient.getAndSetBiliCookie();
            for (UploadDetail uploadDetail : uploadDetails) {
                UploadJob.Context context = new UploadJob.Context();
                SimpleVideoInfo video = new SimpleVideoInfo();
                video.setBvid(uploadDetail.getBvid());
                video.setCid(uploadDetail.getCid());
                context.bilibiliFullVideo = bilibiliClient.getFullVideoBySimpleVideo(video, cookie);
                String s = handleUploadName(context, uploadDetail);
                result.add(s);
            }
            return result;
        }
        return List.of();
    }
}
