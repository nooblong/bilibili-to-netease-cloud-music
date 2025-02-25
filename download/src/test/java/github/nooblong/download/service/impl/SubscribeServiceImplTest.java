package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.Subscribe;
import github.nooblong.download.entity.UploadDetail;
import github.nooblong.download.job.UploadJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

class SubscribeServiceImplTest extends BaseTest {

    @Autowired
    private SubscribeServiceImpl subscribeService;

    @Test
    void subscribe() throws InterruptedException {
        subscribeService.checkAndSave();
        System.out.println("执行完成...");
        Thread.sleep(1000000);
    }

    @Test
    void nextFromSubscribe() {
        Map<String, String> availableBilibiliCookie = bilibiliClient.getAndSetBiliCookie();
        Subscribe subscribe = subscribeService.getById(666);
        subscribeService.checkSubscribe(subscribe, availableBilibiliCookie);
    }

    @Test
    void testHandleUploadName() {
        UploadJob.Context context = new UploadJob.Context();
        context.uploadDetailId = 1L;
        SimpleVideoInfo video = new SimpleVideoInfo();
        video.setBvid("BV1vhADevEgB");
        Map<String, String> cookie = bilibiliClient.getAndSetBiliCookie();
        context.bilibiliFullVideo = bilibiliClient.init(video, cookie);
        UploadDetail uploadDetail = new UploadDetail();
        uploadDetail.setSubscribeId(0L);

        String s = uploadJob.handleUploadName(context, uploadDetail);
        System.out.println(s);
    }

}