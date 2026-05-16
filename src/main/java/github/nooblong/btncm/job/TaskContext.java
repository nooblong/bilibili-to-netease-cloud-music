package github.nooblong.btncm.job;

import github.nooblong.btncm.bilibili.BilibiliFullVideo;
import github.nooblong.btncm.entity.SysUser;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class TaskContext {

    public Path musicPath;
    public String desc = "";
    public String netImageId;
    public BilibiliFullVideo bilibiliFullVideo;
    public Long uploadDetailId;
    public SysUser sysUser;

    private Long taskId;

    private LocalDateTime startTime =
            LocalDateTime.now();

    private LocalDateTime endTime;

    private final List<TaskLog> logs =
            new CopyOnWriteArrayList<>();

    public String getAllLogsText() {

        StringBuilder sb = new StringBuilder();

        for (TaskLog log : logs) {

            sb.append(log.getFormattedText());

            if (log.getException() != null) {

                sb.append("\n");

                sb.append(log.getException());
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}