package github.nooblong.download.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.download.entity.UploadDetail;

import java.util.List;

/**
 * @author lyl
 * @description 针对表【upload_detail】的数据库操作Service
 * @createDate 2023-09-19 14:46:56
 */
public interface UploadDetailService extends IService<UploadDetail> {
    void checkAllAuditStatus();

    boolean isUnique(String uniqueSourceId, String secondUniqueSourceId, Long voiceListId);

    boolean hasUploaded(Long userId);

    void uploadAllOnlySelfSee(Long voiceListId);

    List<UploadDetail> listAllWait();

    void logNow(Long uploadDetailId, String content);
}
