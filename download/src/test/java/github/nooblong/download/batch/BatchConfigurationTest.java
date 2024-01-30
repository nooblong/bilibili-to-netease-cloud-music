package github.nooblong.download.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.common.entity.SysUser;
import github.nooblong.common.util.JwtUtil;
import github.nooblong.download.BaseTest;
import github.nooblong.download.utils.Constant;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
class BatchConfigurationTest extends BaseTest {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    JobOperator jobOperator;

    @Autowired
    JobExplorer jobExplorer;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    OkHttpClient okHttpClient;

    @Test
    void stopJob() throws Exception {
        Set<Long> getUpJob1 = jobOperator.getRunningExecutions("getUpJob");
        Long next = getUpJob1.iterator().next();
        jobOperator.restart(next);
        System.err.println(getUpJob1);
        jobOperator.stop(next);
    }

    @Test
    void removeJob() throws Exception {
        Set<JobExecution> getUpJob1 = jobExplorer.findRunningJobExecutions("getUpJob");
        JobExecution next = getUpJob1.iterator().next();
        next.setEndTime(LocalDateTime.now());
        next.upgradeStatus(BatchStatus.FAILED);
        jobRepository.update(next);
    }

    @BeforeEach
    public void setUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String s = JwtUtil.generateTokenByRS256(new SysUser().setId(1L).setUsername("admin").setPassword("123"));
        request.addHeader("Access-Token", s);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testThrowInRetry() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(6)
                .fixedBackoff(1000)
                .retryOn(Exception.class)
                .withListener(new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                        log.info("失败重试");
                    }
                })
                .build();
        Map<String, String> params = new HashMap<>();
        params.put("uid", "16662103");
        Request request = OkUtil.get(Constant.FULL_BILI_API + "/user/User/get_videos", params);
        JsonNode response = retryTemplate.execute(context -> {
            JsonNode responseIn = OkUtil.getJsonResponse(request, okHttpClient);
            Assert.isTrue(responseIn.has("status"), "获取视频失败");
            return responseIn;
        }, context -> new ObjectMapper().createObjectNode().put("recovery", "true"));
        log.info(response.toString());
    }

}