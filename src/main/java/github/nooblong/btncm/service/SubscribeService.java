package github.nooblong.btncm.service;


import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.btncm.bilibili.SimpleVideoInfo;
import github.nooblong.btncm.entity.Subscribe;
import github.nooblong.btncm.entity.UploadDetail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author lyl
 * @description 针对表【subscribe】的数据库操作Service
 * @createDate 2023-10-13 11:19:19
 */
public interface SubscribeService extends IService<Subscribe> {

    /**
     * 检查所有订阅并保存待上传视频到数据库
     */
    void checkAndSave();

    /**
     * 根据播客列表检查所有订阅并保存待上传视频到数据库
     */
    void checkAndSave(Long userId, Long voiceListId);

    /**
     * 同步所有订阅的up主名和头像
     */
    void syncUpNameAndImage();

    /**
     * 检查单个订阅并保存待上传视频到数据库
     */
    void checkSubscribe(Subscribe subscribe, Map<String, String> availableBilibiliCookie);

    /**
     * 测试订阅的上传名字用
     */
    List<UploadDetail> testProcess(Subscribe subscribe, Iterator<SimpleVideoInfo> iterator, int times);

    /**
     * 删除失效的网易云cookie
     */
    void removeUselessCookie();
}
