package github.nooblong.download.batch;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.bilibili.BilibiliVideo;
import github.nooblong.download.entity.UploadDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UploadSingleAudioGetDataStep {
    @Bean
    public Step getUploadData(JobRepository jobRepository,
                              PlatformTransactionManager platformTransactionManager,
                              BilibiliUtil bilibiliUtil) {
        return new StepBuilder("getUploadData", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters jobParameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
                    String url = jobParameters.getString("url");
                    String cid = jobParameters.getString("cid");
                    assert url != null;
                    BilibiliVideo bilibiliVideo = bilibiliUtil.createByUrl(url);
                    if (StrUtil.isNotEmpty(cid)) {
                        bilibiliVideo.setCid(cid);
                    }
                    bilibiliUtil.init(bilibiliVideo, bilibiliUtil.getCurrentCred());

                    log.info("getUploadData: {}", bilibiliVideo.getTitle());
                    Path path = bilibiliUtil.downloadFile(bilibiliVideo, bilibiliUtil.getCurrentCred());
                    Path imagePath = bilibiliUtil.downloadCover(bilibiliVideo);

                    BilibiliVideoContext bilibiliVideoContext = new BilibiliVideoContext();
                    BeanUtils.copyProperties(bilibiliVideo, bilibiliVideoContext);
                    bilibiliVideoContext.setPath(path.toString());
                    bilibiliVideoContext.setImagePath(imagePath.toString());
                    bilibiliVideoContext.setDescList(new ArrayList<>());
                    ExecutionContext jobContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
                    jobContext.put("bilibiliVideoContext", bilibiliVideoContext);
                    return null;
                }, platformTransactionManager)
                .allowStartIfComplete(true)
                .build();
    }

}
