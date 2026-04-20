package github.nooblong.btncm.service;

import github.nooblong.btncm.entity.UserVoicelist;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserVoicelistService extends IService<UserVoicelist> {

    void syncUserVoicelist();

    void syncUserVoicelist(Long userId);

}
