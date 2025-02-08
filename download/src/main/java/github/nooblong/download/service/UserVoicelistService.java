package github.nooblong.download.service;

import github.nooblong.download.entity.UserVoicelist;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserVoicelistService extends IService<UserVoicelist> {

    void syncUserVoicelist();

    void syncUserVoicelist(Long userId);

}
