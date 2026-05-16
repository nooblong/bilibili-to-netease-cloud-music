package github.nooblong.btncm.job;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskLog {

    private LocalDateTime time;

    private String level;

    private String formattedText;

    private String exception;
}
