package github.nooblong.download.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.model.DeployedContainerInfo;
import tech.powerjob.common.model.SystemMetrics;

import java.util.List;

@Data
@Slf4j
public class WorkerInfo {

    private String address;

    private long lastActiveTime;

    private String protocol;

    private String client;

    private String tag;

    private int lightTaskTrackerNum;

    private int heavyTaskTrackerNum;

    private long lastOverloadTime;

    private boolean overloading;

    private SystemMetrics systemMetrics;

    private List<DeployedContainerInfo> containerInfos;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public boolean timeout() {
        long timeout = System.currentTimeMillis() - lastActiveTime;
        return timeout > WORKER_TIMEOUT_MS;
    }

    public boolean overload() {
        return overloading;
    }

}

