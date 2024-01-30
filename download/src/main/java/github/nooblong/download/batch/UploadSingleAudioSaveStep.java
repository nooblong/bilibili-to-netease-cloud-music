package github.nooblong.download.batch;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.download.entity.UploadDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadSingleAudioSaveStep {
    @Bean
    public Step saveData(JobRepository jobRepository,
                         PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("saveData", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("保存数据");
                    ExecutionContext jobContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
                    JobParameters jobParameters = contribution.getStepExecution().getJobParameters();
                    BilibiliVideoContext bilibiliVideoContext = (BilibiliVideoContext) jobContext.get("bilibiliVideoContext");
                    assert bilibiliVideoContext != null;

                    UploadDetail byId = Db.getById(jobParameters.getLong("id"), UploadDetail.class);
                    Path path = Paths.get(bilibiliVideoContext.getPath());
                    byId.setBvid(bilibiliVideoContext.getBvid());
                    byId.setCid(bilibiliVideoContext.getCid());
                    byId.setLocalName(path.getFileName().toString());
                    byId.setUploadName(bilibiliVideoContext.getUploadName());
                    byId.setVoiceId(Long.valueOf(bilibiliVideoContext.getVoiceId()));
                    byId.setStatus("FINISHED");

                    ObjectMapper objectMapper = new ObjectMapper();
                    String json = objectMapper.writeValueAsString(bilibiliVideoContext);
                    byId.setVideoInfo(json);
                    Db.updateById(byId);

                    jobContext.put("voiceId", bilibiliVideoContext.getVoiceId());

                    // 清理数据
                    log.info("清理数据");
                    Path path1 = Paths.get(bilibiliVideoContext.getPath());
                    try (Stream<Path> walk = Files.walk(path1.getParent())) {
                        walk.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .peek(System.out::println)
                                .forEach(file -> {
                                    if (!file.equals(path1.getParent().toFile())) {
                                        boolean delete = file.delete();
                                        if (!delete) {
                                            log.error("删除失败: {}", file.getName());
                                        }
                                    }
                                });
                    }

                    return null;
                }, platformTransactionManager)
                .allowStartIfComplete(true)
                .build();
    }

}
