package github.nooblong.download.batch;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class UploadSingleAudioUploadMusicStep {
    @Bean
    public Step uploadMusic(JobRepository jobRepository,
                            PlatformTransactionManager platformTransactionManager,
                            NetMusicClient netMusicClient) {
        return new StepBuilder("uploadMusic", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
                    ExecutionContext jobContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
                    BilibiliVideoContext bilibiliVideoContext = (BilibiliVideoContext) jobContext.get("bilibiliVideoContext");
                    assert bilibiliVideoContext != null;

                    String voiceListId = String.valueOf(jobParameters.getLong("voiceListId"));
                    Long uploadUserId = jobParameters.getLong("uploadUserId");

                    String pathStr = bilibiliVideoContext.getPath();

                    log.info("上传: {}", pathStr);
                    Path path = Paths.get(pathStr);

                    List<String> descList = bilibiliVideoContext.getDescList();
                    assert descList != null;
                    StringBuilder desc = new StringBuilder();
                    descList.add("视频bvid: " + bilibiliVideoContext.getBvid());
                    descList.add("b站作者: " + bilibiliVideoContext.getAuthor());
                    descList.add("upload by nooblong/bilibili-to-netease-cloud-music");
                    descList.forEach(s -> desc.append(s).append("\n"));

                    String uploadName = bilibiliVideoContext.getUploadName();
                    if (StrUtil.isNotBlank(jobParameters.getString("customUploadName"))) {
                        uploadName = jobParameters.getString("customUploadName");
                    }

                    Assert.notNull(uploadName, "上传名字为空");
                    if (uploadName.length() > 40) {
                        uploadName = uploadName.substring(0, 40);
                    }

                    String ext = BilibiliUtil.getFileExt(path.getFileName().toString());
                    JsonNode voiceListDetail;
                    if (uploadUserId == null) {
                        voiceListDetail = netMusicClient.getVoiceListDetail(voiceListId);
                    } else {
                        voiceListDetail = netMusicClient.getVoiceListDetailByUserId(voiceListId, uploadUserId);
                    }
                    String categoryId = voiceListDetail.get("categoryId").asText();
                    String secondCategoryId = voiceListDetail.get("secondCategoryId").asText();
                    String coverImgId = voiceListDetail.get("coverImgId").asText();
                    String netImageId = bilibiliVideoContext.getNetImageId() != null ? bilibiliVideoContext.getNetImageId() : coverImgId;

                    String voiceId;
                    if (uploadUserId != null) {
                        voiceId = doUpload(netMusicClient, ext, uploadName, path, voiceListId, netImageId,
                                categoryId, secondCategoryId, desc.toString(), uploadUserId);
                    } else {
                        voiceId = doUploadByHttpUser(netMusicClient, ext, uploadName, path, voiceListId, netImageId,
                                categoryId, secondCategoryId, desc.toString());
                    }
                    assert voiceId != null;
                    bilibiliVideoContext.setVoiceId(voiceId);
                    jobContext.put("bilibiliVideoContext", bilibiliVideoContext);
                    return null;
                }, platformTransactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    private static String doUpload(NetMusicClient netMusicClient, String ext, String uploadName, Path path,
                                   String voiceListId, String coverImgId, String categoryId,
                                   String secondCategoryId, String description, Long userId) {
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
        queryMap.put("privacy", "false");
        queryMap.put("description", description);

        JsonNode audiouploadthird = netMusicClient.getMusicDataByUserId(queryMap, "audiouploadthird", userId);

        JsonNode audioprecheck = netMusicClient.getMusicDataByUserId(queryMap, "audioprecheck", userId);
        Assert.isTrue(audioprecheck.get("code").asInt() == 200, "声音发布失败");

        JsonNode audioupload = netMusicClient.getMusicDataByUserId(queryMap, "audiosubmit", userId);
        Assert.isTrue(audioupload.get("code").asInt() == 200, "声音发布失败");
        ArrayNode result = (ArrayNode) audioupload.get("data");
        return result.get(0).asText();
    }

    private static String doUploadByHttpUser(NetMusicClient netMusicClient, String ext, String uploadName, Path path,
                                             String voiceListId, String coverImgId, String categoryId,
                                             String secondCategoryId, String description) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("ext", ext);
        queryMap.put("fileName", UUID.randomUUID().toString().substring(0, 10) + uploadName);
        JsonNode audiouploadalloc = netMusicClient.getMusicDataByContext(queryMap, "audiouploadalloc");

        String docId = audiouploadalloc.get("result").get("docId").asText();
        String objectKey = audiouploadalloc.get("result").get("objectKey").asText();
        String token = audiouploadalloc.get("result").get("token").asText();
        queryMap.put("docId", docId);
        queryMap.put("objectKey", objectKey);
        queryMap.put("token", token);

        JsonNode audiouploadfirst = netMusicClient.getMusicDataByContext(queryMap, "audiouploadfirst");
        String uploadId = audiouploadfirst.get("uploadId").asText();
        queryMap.put("uploadId", uploadId);

        try {
            queryMap.put("dataInputStream", new FileInputStream(path.toFile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonNode audiouploadsecond = netMusicClient.getMusicDataByContext(queryMap, "audiouploadsecond");

        queryMap.put("name", uploadName);
        queryMap.put("uploadResult", audiouploadsecond);
        queryMap.put("voiceListId", voiceListId);
        queryMap.put("coverImgId", coverImgId);
        queryMap.put("categoryId", categoryId);
        queryMap.put("secondCategoryId", secondCategoryId);
        queryMap.put("privacy", "false");
        queryMap.put("description", description);

        JsonNode audiouploadthird = netMusicClient.getMusicDataByContext(queryMap, "audiouploadthird");

        JsonNode audioprecheck = netMusicClient.getMusicDataByContext(queryMap, "audioprecheck");
        Assert.isTrue(audioprecheck.get("code").asInt() == 200, "声音发布失败");

        JsonNode audioupload = netMusicClient.getMusicDataByContext(queryMap, "audiosubmit");
        Assert.isTrue(audioupload.get("code").asInt() == 200, "声音发布失败");
        ArrayNode result = (ArrayNode) audioupload.get("data");
        return result.get(0).asText();
    }

}
