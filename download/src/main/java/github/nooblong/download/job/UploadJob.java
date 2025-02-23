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
import github.nooblong.download.UploadStatusTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
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
            availableBilibiliCookie = bilibiliClient.getAvailableBilibiliCookie();
        } catch (RuntimeException e) {
            log.info("准备下载:{}: 没有可用b站cookie", uploadDetailList.get(0).getTitle());
            return;
        }
        log.info("开始下载:{}...", uploadDetailList.get(0).getTitle());
        this.process(uploadDetailList.get(0).getId(), availableBilibiliCookie);
    }

    public static class Context {
        Path musicPath;
        String desc = "";
        String netImageId;
        BilibiliFullVideo bilibiliFullVideo;
        Long uploadDetailId;
    }

    public void process(Long uploadDetailId, Map<String, String> availableBilibiliCookie) {

        UploadDetail uploadDetail = uploadDetailService.getById(uploadDetailId);
        Assert.notNull(uploadDetail, "uploadDetail为空");

        // 先更新了信息先，其他不管
        uploadDetail.setUploadRetryTimes(uploadDetail.getUploadRetryTimes() + 1);
        uploadDetail.setUploadStatus(UploadStatusTypeEnum.PROCESSING);
        uploadDetailService.updateById(uploadDetail);

        Context context = new Context();
        context.uploadDetailId = uploadDetailId;
        try {
            // 收集错误信息
            getData(context, uploadDetail.getBvid(), uploadDetail.getCid(),
                    uploadDetail.getUseVideoCover() == 1, uploadDetail.getUserId(), availableBilibiliCookie);
            codecAudio(context, uploadDetail.getBeginSec(), uploadDetail.getEndSec(), uploadDetail.getOffset());
            // 上传之前先设置名字
            uploadDetail.setUploadName(handleUploadName(context, uploadDetail));
            String voiceId = uploadNetease(context, String.valueOf(uploadDetail.getVoiceListId()),
                    uploadDetail.getUserId(),
                    uploadDetail.getUploadName(), uploadDetail.getPrivacy());
            clear(context, Long.valueOf(voiceId));

            uploadDetailService.logNow(context.uploadDetailId, ">>> 单曲上传成功, 声音id: " + voiceId);
        } catch (Exception e) {
            uploadDetail.setUploadStatus(UploadStatusTypeEnum.ERROR);
            if (uploadDetail.getUploadRetryTimes() > Constant.UPLOAD_MAX_RETRY_TIMES) {
                uploadDetail.setUploadStatus(UploadStatusTypeEnum.MAX_RETRY);
            }
            Db.updateById(uploadDetail);
            uploadDetailService.logNow(context.uploadDetailId, ">>> 声音上传失败: " + e.getMessage());
            delete(context);
            uploadDetailService.logNow(context.uploadDetailId, ">>> 垃圾文件清理成功: " + e.getMessage());
        }
    }

    private void getData(Context context, String bvid, String cid, boolean useVideoCover, Long userId,
                         Map<String, String> availableBilibiliCookie) {
        assert bvid != null;
        assert !useVideoCover || userId != null && userId != 0;
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(bvid);
        if (StrUtil.isNotEmpty(cid)) {
            simpleVideoInfo.setCid(cid);
        }
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, availableBilibiliCookie);
        context.bilibiliFullVideo = bilibiliFullVideo;
        context.musicPath = bilibiliClient.downloadFile(bilibiliFullVideo, availableBilibiliCookie);
        if (useVideoCover) {
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

    private void codecAudio(Context context, double beginSec, double endSec, double voiceOffset) {
        Path targetPath = ffmpegService.encodeMp3(context.musicPath, beginSec, endSec, voiceOffset);
        String ext = CommonUtil.getFileExt(context.musicPath.getFileName().toString());
        context.musicPath = targetPath;
        String s1 = "编码:" + ext;
        try {
            log.info("转码后的文件大小: {}K", Files.size(targetPath) / 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        uploadDetailService.logNow(context.uploadDetailId, ">> " + s1 + "音频转码成功");
    }

    private String uploadNetease(Context context, String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
        uploadDetailService.logNow(context.uploadDetailId, ">>> 开始上传网易云");
        String toAddDesc = "";
        toAddDesc += ("\n视频bvid: " + context.bilibiliFullVideo.getBvid());
        toAddDesc += ("\nb站作者: " + context.bilibiliFullVideo.getAuthor());
        toAddDesc += ("\n一键上传工具: www.nooblong.tech");
        toAddDesc += ("\ngithub: nooblong/bilibili-to-netease-cloud-music");
        context.desc += toAddDesc;
        uploadDetailService.logNow(context.uploadDetailId, ">>> 添加介绍: " +
                toAddDesc.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\t", " "));

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
}
