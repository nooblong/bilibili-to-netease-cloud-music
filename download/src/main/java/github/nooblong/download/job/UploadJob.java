package github.nooblong.download.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.SubscribeReg;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.service.SubscribeRegService;
import github.nooblong.download.service.SubscribeService;
import github.nooblong.download.service.UploadDetailService;
import github.nooblong.download.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadJob implements BasicProcessor {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final FfmpegService ffmpegService;
    final SubscribeRegService subscribeRegService;
    final SubscribeService subscribeService;
    final UploadDetailService uploadDetailService;

    Path musicPath;
    String desc = "";
    String netImageId;
    BilibiliFullVideo bilibiliFullVideo;

    public UploadJob(BilibiliClient bilibiliClient,
                     NetMusicClient netMusicClient,
                     FfmpegService ffmpegService,
                     SubscribeRegService subscribeRegService,
                     SubscribeService subscribeService,
                     UploadDetailService uploadDetailService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.ffmpegService = ffmpegService;
        this.subscribeRegService = subscribeRegService;
        this.subscribeService = subscribeService;
        this.uploadDetailService = uploadDetailService;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger logger = context.getOmsLogger();

        String param;
        //（允许动态[instanceParams]覆盖静态参数[jobParams]）
        param = StringUtils.isBlank(context.getInstanceParams()) ? context.getJobParams() : context.getInstanceParams();

        ObjectMapper objectMapper = new ObjectMapper();
        UploadDetail uploadDetail = objectMapper.readValue(param, UploadDetail.class);

        // 先更新了信息先，其他不管
        Long instanceId = context.getInstanceId();
        uploadDetail.setRetryTimes(uploadDetail.getRetryTimes() + 1);
        uploadDetail.setStatus(StatusTypeEnum.PROCESSING);
        uploadDetail.setInstanceId(instanceId);
        uploadDetailService.updateById(uploadDetail);
        try {
            // 收集错误信息
            getData(logger, uploadDetail.getBvid(), uploadDetail.getCid(),
                    uploadDetail.getUseVideoCover() == 1, uploadDetail.getUserId());
            codecAudio(logger, uploadDetail.getBeginSec(), uploadDetail.getEndSec(), uploadDetail.getOffset());
            // 上传之前先设置名字
            uploadDetail.setUploadName(handleUploadName(uploadDetail));
            String voiceId = uploadNetease(logger, String.valueOf(uploadDetail.getVoiceListId()), uploadDetail.getUserId(),
                    uploadDetail.getUploadName(), uploadDetail.getPrivacy());
            clear(logger, uploadDetail, Long.valueOf(voiceId));

            logger.info("单曲上传成功, 声音id:[{}]", voiceId);
            return new ProcessResult(true, "单曲上传成功, 声音id:[" + voiceId + "]");
        } catch (Exception e) {
            uploadDetail.setStatus(StatusTypeEnum.INTERNAL_ERROR);
            Db.updateById(uploadDetail);
            log.error(e.getMessage());
            logger.error("声音上传失败: {}", e.getMessage());
            return new ProcessResult(false, "单曲上传失败: " + e.getMessage());
        }
    }

    private void getData(OmsLogger log, String bvid, String cid, boolean useVideoCover, Long userId) {
        assert bvid != null;
        assert !useVideoCover || userId != null && userId != 0;
        SimpleVideoInfo simpleVideoInfo = bilibiliClient.createByUrl(bvid);
        if (StrUtil.isNotEmpty(cid)) {
            simpleVideoInfo.setCid(cid);
        }
        BilibiliFullVideo bilibiliFullVideo = bilibiliClient.init(simpleVideoInfo, bilibiliClient.getCurrentCred());
        this.bilibiliFullVideo = bilibiliFullVideo;
        musicPath = bilibiliClient.downloadFile(bilibiliFullVideo, bilibiliClient.getCurrentCred());
        if (useVideoCover) {
            Path imagePath = bilibiliClient.downloadCover(bilibiliFullVideo);
            log.info("下载封面成功");
            this.netImageId = transImage(log, imagePath, netMusicClient, userId);
        }
    }

    private String transImage(OmsLogger log, Path path, NetMusicClient netMusicClient, Long userId) {
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
        log.info("裁剪封面成功", imageuploadsecond.toString());
        return imageuploadsecond.get("id").asText();

    }

    private void codecAudio(OmsLogger log, double beginSec, double endSec, double voiceOffset) {
//        long bitRate1 = ffmpegService.probeInfo(musicPath).getFormat().bit_rate / 1000;
        Path targetPath = ffmpegService.encodeMp3(musicPath, beginSec, endSec, voiceOffset);
//        long bitRate2 = ffmpegService.probeInfo(targetPath).getFormat().bit_rate / 1000;
        String ext = BilibiliClient.getFileExt(musicPath.getFileName().toString());
        this.musicPath = targetPath;
        String s1 = "编码:" + ext + "->" + Constant.FFMPEG_FORMAT_MP3;
        String s2 = "码率:" + 1 + "kbps" + "->" + 1 + "kbps";
        desc += s1;
        desc += "\n";
        desc += s2;
        log.info("音频转码成功");
    }

    private String uploadNetease(OmsLogger log, String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
        log.info("开始上传网易云");
        desc += ("\n视频bvid: " + bilibiliFullVideo.getBvid());
        desc += ("\nb站作者: " + bilibiliFullVideo.getAuthor());
        desc += ("\n一键上传工具: www.nooblong.tech");
        desc += ("\ngithub: nooblong/bilibili-to-netease-cloud-music");

        Assert.notNull(uploadName, "上传名字为空");
        if (uploadName.length() > 40) {
            uploadName = uploadName.substring(0, 40);
        }

        String ext = BilibiliClient.getFileExt(musicPath.getFileName().toString());
        JsonNode voiceListDetail = netMusicClient.getVoiceListDetailByUserId(voiceListId, uploadUserId);
        String categoryId = voiceListDetail.get("categoryId").asText();
        String secondCategoryId = voiceListDetail.get("secondCategoryId").asText();
        String coverImgId = voiceListDetail.get("coverImgId").asText();
        String netImageId = this.netImageId != null ? this.netImageId : coverImgId;

        String voiceId = doUpload(netMusicClient, ext, uploadName, musicPath, voiceListId, netImageId,
                categoryId, secondCategoryId, desc, uploadUserId,
                privacy == 1 ? "true" : "false");
        Assert.notNull(voiceId, "返回的声音id为空");
        return voiceId;
    }

    private static String doUpload(NetMusicClient netMusicClient, String ext, String uploadName, Path path,
                                   String voiceListId, String coverImgId, String categoryId,
                                   String secondCategoryId, String description, Long userId, String privacy) {
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

    private void clear(OmsLogger log, UploadDetail uploadDetail, Long voiceId) {
        uploadDetail.setVoiceId(voiceId);
        uploadDetail.setStatus(StatusTypeEnum.AUDITING);
        Db.updateById(uploadDetail);
        log.info("数据库数据已更新");
        // 清理数据
        try (Stream<Path> walk = Files.walk(musicPath.getParent())) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(file -> {
                        if (!file.equals(musicPath.getParent().toFile())) {
                            log.info("删除文件: {}", file.getName());
//                            boolean delete = file.delete();
//                            if (!delete) {
//                                log.error("删除失败: {}", file.getName());
//                            }
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String handleUploadName(UploadDetail uploadDetail) {
        // 有自定义，一般为单曲上传
        if (StrUtil.isNotBlank(uploadDetail.getUploadName())) {
            return uploadDetail.getUploadName();
        }
        // 检查是否有正则要求
        List<SubscribeReg> subscribeRegs = subscribeRegService
                .lambdaQuery().eq(SubscribeReg::getSubscribeId, uploadDetail.getSubscribeId()).list();
        if (!subscribeRegs.isEmpty()) {
            // 有正则
            Subscribe subscribe = subscribeService.getById(uploadDetail.getSubscribeId());
            String regName = subscribe.getRegName();
            String toRegTitle = bilibiliFullVideo.getHasMultiPart()
                    ? bilibiliFullVideo.getPartName() + "-" + bilibiliFullVideo.getTitle()
                    : bilibiliFullVideo.getTitle();
            if (regName != null) {
                Map<Integer, String> replaceMap = new HashMap<>();
                for (SubscribeReg subscribeReg : subscribeRegs) {
                    // 先读取了原标题的reg生成map
                    try {
                        String s1 = ReUtil.extractMulti(subscribeReg.getRegex(), toRegTitle, "$1");
                        if (s1 != null) {
                            replaceMap.put(subscribeReg.getPos(), s1);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
                // 利用map替换subscribe的Name
                String result = ReUtil.replaceAll(regName, "\\{(.*?)}", match -> {
                    String content = match.group(0);
                    content = content.substring(1, content.length() - 1);
                    if (content.equals("pubdate")) {
                        Date videoCreateTime = bilibiliFullVideo.getVideoCreateTime();
                        return DateUtil.format(videoCreateTime, "yyyy.MM.dd");
                    } else if (NumberUtil.isNumber(content)) {
                        if (replaceMap.containsKey(Integer.valueOf(content))) {
                            return replaceMap.getOrDefault(Integer.valueOf(content), "");
                        }
                    } else {
                        return content;
                    }
                    return content;
                });
                return result;
            } else {
                // 有正则，但是没填regName
                return bilibiliFullVideo.getTitle();
            }
        } else {
            // 无正则，uploadName就使用 视频名-分p名 或 视频名
            if (bilibiliFullVideo.getHasMultiPart()) {
                return bilibiliFullVideo.getPartName() + "-" + bilibiliFullVideo.getTitle();
            } else {
                return bilibiliFullVideo.getTitle();
            }
        }
    }

}
