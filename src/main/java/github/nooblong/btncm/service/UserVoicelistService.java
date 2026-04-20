package github.nooblong.btncm.service;

import github.nooblong.btncm.entity.UserVoicelist;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserVoicelistService extends IService<UserVoicelist> {

    /**
     * 同步用户的播客列表
     */
    void syncUserVoicelist();

    /**
     * 同步特定id的播客列表
     */
    void syncUserVoicelist(Long userId);

}
