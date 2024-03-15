package github.nooblong.download.batch;

import github.nooblong.common.entity.SysUser;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.BilibiliUtil;
import github.nooblong.download.netmusic.NetMusicClient;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

class UploadSingleAudioTaskTest extends BaseTest {

    @Autowired
    JobOperator jobOperator;

    @Autowired
    JobExplorer jobExplorer;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    @Qualifier("uploadSingleAudioJob")
    Job uploadSingleAudioJob;

    @Autowired
    NetMusicClient netMusicClient;

    @Autowired
    BilibiliUtil bilibiliUtil;

    @Deprecated
    @Test
    public void testUploadSingleAudioTask() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        String s = JwtUtil.generateTokenByRS256(new SysUser().setId(1L).setUsername("admin").setPassword("123"));
        request.addHeader("Access-Token", s);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Long voiceListId = 994819294L; // 测试
//        String voiceListId = "996002302";// 自动搬运

        // bilibili
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("url", "https://www.bilibili.com/video/BV1HK41187ud/")
//                .addString("cid", "478776908")

                // req
//                .addDouble("beginSec", 10D)
//                .addDouble("endSec", 40D)
//                .addDouble("voiceOffset", 0.1)
//                .addString("customUploadName",
//                        "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十")

                // music
                .addLong("voiceListId", voiceListId)
                .addString("useDefaultImg", "false")
//                .addString("customUploadName", "测试")

                .addLong("crack", 0L)
                .addLong("useVideoCover", 0L)
                .addString("privacy", "true")

                .addDate("time", new Date())
                .toJobParameters();

        jobLauncher.run(uploadSingleAudioJob, jobParameters);
    }

}