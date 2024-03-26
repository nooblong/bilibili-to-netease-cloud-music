package github.nooblong.download.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.nooblong.download.entity.WorkerStatus;
import github.nooblong.download.utils.OkUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JobUtil implements InitializingBean {

    private static final OkHttpClient client = new OkHttpClient();

    @Value("${powerjob.worker.server-address}")
    public void setAddress(String address) {
        JobUtil.address = address;
    }

    @Value("${powerjob.worker.password}")
    public void setPassword(String password) {
        JobUtil.password = password;
    }

    @Value("${powerjob.worker.app-name}")
    public void setName(String name) {
        JobUtil.name = name;
    }

    public static String address;
    public static String password;
    public static String name;

    public static List<String> listWorkersAddrAvailable() {
        try {
            JsonNode jsonResponse = OkUtil.getJsonResponseNoLog(
                    OkUtil.getNoLog("http://" + address + "/system/listWorker?appId=1"), client);
            Assert.isTrue(jsonResponse.get("success").asBoolean(), "查询worker失败=false");
            WorkerStatus[] data = new ObjectMapper().convertValue(jsonResponse.get("data"), WorkerStatus[].class);
            List<String> result = new ArrayList<>();
            for (WorkerStatus workerStatus : data) {
                if (workerStatus.getStatus() != 9999
                        && workerStatus.getLightTaskTrackerNum() == 0
                        && workerStatus.getHeavyTaskTrackerNum() == 0) {
                    result.add(workerStatus.getAddress());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("查询worker失败", e);
        }
        return new ArrayList<>();
    }

    public static List<WorkerStatus> workerStatusList() {
        JsonNode jsonResponse = OkUtil.getJsonResponseNoLog(
                OkUtil.getNoLog("http://" + address + "/system/listWorker?appId=1"), client);
        Assert.isTrue(jsonResponse.get("success").asBoolean(), "查询worker失败=false");
        WorkerStatus[] data = new ObjectMapper().convertValue(jsonResponse.get("data"), WorkerStatus[].class);
        return Arrays.stream(data).toList();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
