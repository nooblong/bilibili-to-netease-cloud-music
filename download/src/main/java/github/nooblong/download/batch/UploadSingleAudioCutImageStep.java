package github.nooblong.download.batch;

import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.netmusic.NetMusicClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Component
@Slf4j
public class UploadSingleAudioCutImageStep {
    @Bean
    public Step cutImage(JobRepository jobRepository,
                         PlatformTransactionManager platformTransactionManager,
                         NetMusicClient netMusicClient) {
        return new StepBuilder("cutImage", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext jobContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
                    BilibiliVideoContext bilibiliVideoContext = (BilibiliVideoContext) jobContext.get("bilibiliVideoContext");
                    assert bilibiliVideoContext != null;
                    String imagePathStr = bilibiliVideoContext.getImagePath();
                    Path imagePath = Paths.get(imagePathStr);
                    Long uploadUserId = contribution.getStepExecution().getJobParameters().getLong("uploadUserId");
                    String netImageId = transImage(imagePath, netMusicClient, uploadUserId);
                    bilibiliVideoContext.setNetImageId(netImageId);
                    jobContext.put("bilibiliVideoContext", bilibiliVideoContext);
                    return null;
                }, platformTransactionManager)
                .allowStartIfComplete(true)
                .build();
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
            throw new RuntimeException("图片读取失败: " + path);
        }
        params.put("imagePath", path.toString());
        if (userId != null) {
            JsonNode imageuploadalloc = netMusicClient.getMusicDataByUserId(params, "imageuploadalloc", userId);
            String docId = imageuploadalloc.get("result").get("docId").asText();
            String objectKey = imageuploadalloc.get("result").get("objectKey").asText();
            String token = imageuploadalloc.get("result").get("token").asText();
            params.put("docId", docId);
            params.put("objectKey", objectKey);
            params.put("token", token);
            netMusicClient.getMusicDataByUserId(params, "imageuploadfirst", userId);
            JsonNode imageuploadsecond = netMusicClient.getMusicDataByUserId(params, "imageuploadsecond", userId);
            log.info("获取的封面id: {}", imageuploadsecond.toString());
            return imageuploadsecond.get("id").asText();
        } else {
            JsonNode imageuploadalloc = netMusicClient.getMusicDataByContext(params, "imageuploadalloc");
            String docId = imageuploadalloc.get("result").get("docId").asText();
            String objectKey = imageuploadalloc.get("result").get("objectKey").asText();
            String token = imageuploadalloc.get("result").get("token").asText();
            params.put("docId", docId);
            params.put("objectKey", objectKey);
            params.put("token", token);
            netMusicClient.getMusicDataByContext(params, "imageuploadfirst");
            JsonNode imageuploadsecond = netMusicClient.getMusicDataByContext(params, "imageuploadsecond");
            log.info("获取的封面id: {}", imageuploadsecond.toString());
            return imageuploadsecond.get("id").asText();
        }
    }

}
