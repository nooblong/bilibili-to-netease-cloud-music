package github.nooblong.download.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysInfo implements Serializable {
    private Integer login163Num;
    private Integer visitTimes;
    private Integer visitToday;
    private Integer visitTodayTimes;
}