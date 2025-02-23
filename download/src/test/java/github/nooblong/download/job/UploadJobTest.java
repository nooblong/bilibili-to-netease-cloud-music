package github.nooblong.download.job;

import github.nooblong.download.BaseTest;
import github.nooblong.download.bilibili.BilibiliFullVideo;
import github.nooblong.download.bilibili.SimpleVideoInfo;
import github.nooblong.download.entity.UploadDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UploadJobTest extends BaseTest {

    @Test
    void testHandleUploadName() {
        UploadJob.Context context = new UploadJob.Context();
        context.uploadDetailId = 1L;
        SimpleVideoInfo video = new SimpleVideoInfo();
        video.setBvid("BV1vhADevEgB");
        Map<String, String> cookie = bilibiliClient.getAvailableBilibiliCookie();
        context.bilibiliFullVideo = bilibiliClient.init(video, cookie);
        UploadDetail uploadDetail = new UploadDetail();
        uploadDetail.setSubscribeId(0L);

        String s = uploadJob.handleUploadName(context, uploadDetail);
        System.out.println(s);
    }

}