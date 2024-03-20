package github.nooblong.download.service;


import com.baomidou.mybatisplus.extension.service.IService;
import github.nooblong.download.entity.Subscribe;
import tech.powerjob.worker.log.OmsLogger;

/**
 * @author lyl
 * @description 针对表【subscribe】的数据库操作Service
 * @createDate 2023-10-13 11:19:19
 */
public interface SubscribeService extends IService<Subscribe> {

    void checkAndSave(OmsLogger log);

}
