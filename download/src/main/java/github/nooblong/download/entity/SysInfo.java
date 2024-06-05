package github.nooblong.download.entity;

import lombok.Data;

@Data
public class SysInfo {
    private boolean netCookieStatus;
    private boolean bilibiliCookieStatus;
    private boolean ready;
    private Integer regNum;
    private Integer annoVisitNum;
    private Integer userVisitNum;
}