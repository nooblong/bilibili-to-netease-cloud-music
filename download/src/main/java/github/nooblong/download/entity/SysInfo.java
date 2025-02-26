package github.nooblong.download.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysInfo implements Serializable {
//    private boolean netCookieStatus;
//    private boolean bilibiliCookieStatus;
//    private boolean ready;
    private Integer regNum;
    private Integer annoVisitNum;
    private Integer userVisitNum;
}