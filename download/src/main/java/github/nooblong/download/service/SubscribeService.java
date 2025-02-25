package github.nooblong.download.service;


import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lyl
 * @description 针对表【subscribe】的数据库操作Service
 * @createDate 2023-10-13 11:19:19
 */
public interface SubscribeService extends IService<Subscribe> {

    void checkAndSave();

    void checkAndSave(Long userId);

    void checkAndSave(Long userId, Long voiceListId);

    void syncUpNameAndImage();

    void checkSubscribe(Subscribe subscribe, Map<String, String> availableBilibiliCookie);

    List<UploadDetail> testProcess(Subscribe subscribe, Iterator<SimpleVideoInfo> iterator, int times);
}
