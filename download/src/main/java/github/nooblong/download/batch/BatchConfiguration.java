package github.nooblong.download.batch;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {
    @Bean
    public Job uploadSingleAudioJob(JobRepository jobRepository,
                                    @Qualifier("getUploadData") Step getUploadData,
                                    @Qualifier("uploadMusic") Step uploadMusic,
                                    @Qualifier("cutImage") Step cutImage,
                                    @Qualifier("codecAudio") Step codecAudio,
                                    @Qualifier("codecCrackAudio") Step codecCrackAudio,
                                    @Qualifier("saveData") Step saveData,
                                    CrackDecider crackDecider,
                                    CutDecider cutDecider
    ) {
        return new JobBuilder("uploadSingleAudioJob", jobRepository)
                .start(getUploadData)
                .next(cutDecider).on("CUT").to(cutImage).next(crackDecider).on("CRACK").to(codecCrackAudio).next(uploadMusic).from(crackDecider).on("NO_CRACK").to(codecAudio)
                .from(cutDecider).on("SKIP").to(crackDecider).on("CRACK").to(codecCrackAudio).next(uploadMusic).from(crackDecider).on("NO_CRACK").to(codecAudio)
                .next(uploadMusic)
                .next(saveData)
                .build()
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(@Nonnull JobExecution jobExecution) {
                        jobExecution.getExecutionContext().put("descList", new ArrayList<String>());
                    }
                })
                .build();
    }

    @Bean
    public CrackDecider crackDecider() {
        return new CrackDecider();
    }

    @Bean
    public CutDecider cutDecider() {
        return new CutDecider();
    }

    public static class CrackDecider implements JobExecutionDecider {
        @NotNull
        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            Long cut = jobExecution.getJobParameters().getLong("crack");
            if (cut != null && cut > 0) {
                return new FlowExecutionStatus("CRACK");
            } else {
                return new FlowExecutionStatus("NO_CRACK");
            }
        }
    }

    public static class CutDecider implements JobExecutionDecider {
        @NotNull
        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            Long cut = jobExecution.getJobParameters().getLong("useVideoCover");
            if (cut != null && cut > 0) {
                return new FlowExecutionStatus("CUT");
            } else {
                return new FlowExecutionStatus("SKIP");
            }
        }
    }

}
