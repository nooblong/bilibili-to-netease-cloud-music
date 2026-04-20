package github.nooblong.btncm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.entity.UploadDetail;

/**
 * @author lyl
 * @description 针对表【upload_detail】的数据库操作Service
 * @createDate 2023-09-19 14:46:56
 */
public interface UploadDetailService extends IService<UploadDetail> {

    void checkAllAuditStatus();

    boolean isUnique(String uniqueSourceId, String secondUniqueSourceId, Long voiceListId);

    UploadDetail getToUploadWithCookie();

}
