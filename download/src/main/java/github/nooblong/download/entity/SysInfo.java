package github.nooblong.download.entity;

import lombok.Data;

import java.util.List;

@Data
public class SysInfo {

    private int id;
    private String activeBilibiliUserName;

    public List<WorkerStatus> getWorkerStatusList() {
        return workerStatusList.stream().peek(workerStatus -> workerStatus.setAddress("")).toList();
    }

    private List<WorkerStatus> workerStatusList;

}
