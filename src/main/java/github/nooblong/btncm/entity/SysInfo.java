package github.nooblong.btncm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SysInfo implements Serializable {
    private Integer login163Num;
    private Integer visitTimes;
    private Integer visitToday;
    private Integer visitTodayTimes;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;
    private Boolean login;
    private List<AfdOrder> afdOrders;
    private List<AfdOrder> myOrders;
    private String afdId;
    private Integer remaining;

    private Integer todayUploadNum;
    private Integer todayUploadSuccessNum;
    private Integer totalUploadNum;
    private Integer todayUploadUserNum;
    private Integer todayHasNewUploadSubscribe;
    private Integer enabledSubscribeNum;
    private Integer enabledSubscribeUserNum;

}