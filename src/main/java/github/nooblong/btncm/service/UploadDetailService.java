package github.nooblong.btncm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.entity.UploadDetail;

/**
 * @author lyl
 * @description 针对表【upload_detail】的数据库操作Service
 * @createDate 2023-09-19 14:46:56
 */
public interface UploadDetailService extends IService<UploadDetail> {

    /**
     * 获取所有上传好的网易云审核状态
     */
    void checkAllAuditStatus();

    /**
     * 是否为唯一的上传任务，避免重复传
     */
    boolean isUnique(String uniqueSourceId, String secondUniqueSourceId, Long voiceListId);

    /**
     * 获取待上传的视频
     */
    UploadDetail getToUploadWithCookie();

    /**
     * 获取今日上传次数
     */
    Long getTodayUploadNum();

    /**
     * 获取总上传数量
     */
    Long getTotalUploadNum();

    /**
     * 获取今日上传成功数量
     */
    Long getTodayUploadSuccessNum();

    /**
     * 获取今日上传用户数
     */
    Long getTodayUploadUserNum();

    /**
     * 获取今日有更新的订阅数
     */
    Long getTodayHasNewUploadSubscribe();

    /**
     * 获取总活跃的订阅数
     */
    Long getEnabledSubscribeNum();

    /**
     * 获取有活跃的订阅数的用户数
     */
    Long getEnabledSubscribeUserNum();

}
