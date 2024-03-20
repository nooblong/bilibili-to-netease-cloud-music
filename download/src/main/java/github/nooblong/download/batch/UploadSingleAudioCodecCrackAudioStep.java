package github.nooblong.download.batch;

import github.nooblong.download.bilibili.BilibiliClient;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class UploadSingleAudioCodecCrackAudioStep {
    @Bean
    public Step codecCrackAudio(JobRepository jobRepository,
                                PlatformTransactionManager platformTransactionManager,
                                FfmpegService ffmpegService) {
        return new StepBuilder("codecAudio", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters jobParameters = contribution.getStepExecution().getJobParameters();
                    ExecutionContext jobContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
                    BilibiliVideoContext bilibiliVideoContext = (BilibiliVideoContext) jobContext.get("bilibiliVideoContext");
                    assert bilibiliVideoContext != null;

                    Path sourceUrl = Paths.get(bilibiliVideoContext.getPath());
                    long bitRate1 = ffmpegService.probeInfo(sourceUrl).getFormat().bit_rate / 1000;
                    Double beginSec = jobParameters.getDouble("beginSec");
                    Double endSec = jobParameters.getDouble("endSec");
                    Double voiceOffset = jobParameters.getDouble("voiceOffset");
                    Path targetPath = ffmpegService.encodeMp3(sourceUrl,
                            beginSec != null ? beginSec : 0,
                            endSec != null ? endSec : 0,
                            voiceOffset != null ? voiceOffset : 0);
                    long bitRate2 = ffmpegService.probeInfo(targetPath).getFormat().bit_rate / 1000;
                    String ext = BilibiliClient.getFileExt(sourceUrl.getFileName().toString());
                    bilibiliVideoContext.setPath(targetPath.toString());
                    String s1 = "编码由" + ext + "转为" + Constant.FFMPEG_FORMAT_MP3;
                    String s2 = "码率由" + bitRate1 + "kbps" + "转为" + bitRate2 + "kbps";
                    List<String> list = bilibiliVideoContext.getDescList();
                    assert list != null;
                    list.add(s1);
                    list.add(s2);
                    jobContext.put("bilibiliVideoContext", bilibiliVideoContext);
                    return null;
                }, platformTransactionManager)
                .allowStartIfComplete(true)
                .build();
    }
}
