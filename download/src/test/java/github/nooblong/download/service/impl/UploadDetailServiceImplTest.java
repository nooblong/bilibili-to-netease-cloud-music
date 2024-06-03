package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import github.nooblong.download.BaseTest;
import github.nooblong.download.entity.UploadDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

class UploadDetailServiceImplTest extends BaseTest {

    @Test
    void setXiaoZSql() {
        List<UploadDetail> list = uploadDetailService.lambdaQuery().eq(UploadDetail::getVoiceListId, 996968337).list();
        for (UploadDetail uploadDetail : list) {
            String uploadName = uploadDetail.getUploadName();
            String[] split = uploadName.split("-");
            assert StrUtil.isNumeric(split[2]);
            uploadDetail.setCid(split[2]);
        }
        uploadDetailService.updateBatchById(list);
    }
}