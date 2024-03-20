package github.nooblong.download.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.download.StatusTypeEnum;
import github.nooblong.download.batch.BilibiliVideoContext;
import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.netmusic.NetMusicClient;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
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
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadJob implements BasicProcessor {

    final BilibiliClient bilibiliClient;
    final NetMusicClient netMusicClient;
    final FfmpegService ffmpegService;

    Path musicPath;
    String desc;
    String netImageId;
    BilibiliVideo bilibiliVideo;

    public UploadJob(BilibiliClient bilibiliClient,
                     NetMusicClient netMusicClient,
                     FfmpegService ffmpegService) {
        this.bilibiliClient = bilibiliClient;
        this.netMusicClient = netMusicClient;
        this.ffmpegService = ffmpegService;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger logger = context.getOmsLogger();

        String jobParams = Optional.ofNullable(context.getJobParams()).orElse("S");
        logger.info("Current context:{}", context.getWorkflowContext());
        logger.info("Current job params:{}", jobParams);
        ObjectMapper objectMapper = new ObjectMapper();
        UploadDetail uploadDetail = objectMapper.readValue(jobParams, UploadDetail.class);

        getData(logger, uploadDetail.getBvid(), uploadDetail.getCid(),
                uploadDetail.getUseVideoCover() == 1, uploadDetail.getUserId());
        if (uploadDetail.getCrack() == 1L) {
            codecAudioCrack(logger, uploadDetail.getBeginSec(), uploadDetail.getEndSec(), uploadDetail.getOffset());
        } else {
            codecAudio(logger, uploadDetail.getBeginSec(), uploadDetail.getEndSec(), uploadDetail.getOffset());
        }

        String voiceId = uploadNetease(logger, String.valueOf(uploadDetail.getVoiceListId()), uploadDetail.getUserId(),
                uploadDetail.getUploadName(), uploadDetail.getPrivacy());

        logger.info("单曲上传成功, 声音id:[{}]", voiceId);
        return new ProcessResult(true, "单曲上传成功, 声音id:[" + voiceId + "]");

    }

    private void getData(OmsLogger log, String bvid, String cid, boolean useVideoCover, Long userId) {
        assert bvid != null;
        assert !useVideoCover || userId != null && userId != 0;
        BilibiliVideo bilibiliVideo = bilibiliClient.createByUrl(bvid);
        if (StrUtil.isNotEmpty(cid)) {
            bilibiliVideo.setCid(cid);
        }
        bilibiliClient.init(bilibiliVideo, bilibiliClient.getCurrentCred());
        this.bilibiliVideo = bilibiliVideo;
        musicPath = bilibiliClient.downloadFile(bilibiliVideo, bilibiliClient.getCurrentCred());
        if (useVideoCover) {
            Path imagePath = bilibiliClient.downloadCover(bilibiliVideo);
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
        long bitRate1 = ffmpegService.probeInfo(musicPath).getFormat().bit_rate / 1000;
        Path targetPath = ffmpegService.encodeMp3(musicPath, beginSec, endSec, voiceOffset);
        long bitRate2 = ffmpegService.probeInfo(targetPath).getFormat().bit_rate / 1000;
        String ext = BilibiliClient.getFileExt(musicPath.getFileName().toString());
        this.musicPath = targetPath;
        String s1 = "编码:" + ext + "->" + Constant.FFMPEG_FORMAT_MP3;
        String s2 = "码率:" + bitRate1 + "kbps" + "->" + bitRate2 + "kbps";
        desc += s1;
        desc += "\n";
        desc += s2;
        log.info("音频转码成功");
    }

    private void codecAudioCrack(OmsLogger log, double beginSec, double endSec, double voiceOffset) {
        long bitRate1 = ffmpegService.probeInfo(musicPath).getFormat().bit_rate / 1000;
        Path targetPath = ffmpegService.encodeMp3(musicPath, beginSec, endSec, voiceOffset);
        long bitRate2 = ffmpegService.probeInfo(targetPath).getFormat().bit_rate / 1000;
        String ext = BilibiliClient.getFileExt(musicPath.getFileName().toString());
        this.musicPath = targetPath;
        String s1 = "编码:" + ext + "->" + Constant.FFMPEG_FORMAT_MP3;
        String s2 = "码率:" + bitRate1 + "kbps" + "->" + bitRate2 + "kbps";
        desc += s1;
        desc += "\n";
        desc += s2;
        log.info("音频转码!成功");
    }

    private String uploadNetease(OmsLogger log, String voiceListId, Long uploadUserId,
                                 String uploadName, long privacy) {
        log.info("开始上传网易云");
        desc += ("\n视频bvid: " + bilibiliVideo.getBvid());
        desc += ("\nb站作者: " + bilibiliVideo.getAuthor());
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
        Assert.isTrue(audioprecheck.get("code").asInt() == 200, "声音发布失败");

        JsonNode audioupload = netMusicClient.getMusicDataByUserId(queryMap, "audiosubmit", userId);
        Assert.isTrue(audioupload.get("code").asInt() == 200, "声音发布失败");
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
                            boolean delete = file.delete();
                            if (!delete) {
                                log.error("删除失败: {}", file.getName());
                            }
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
