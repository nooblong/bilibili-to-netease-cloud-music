package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import github.nooblong.download.service.FfmpegService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;

class FfmpegServiceImplTest extends BaseTest {

    @Autowired
    FfmpegService ffmpegService;

    @Test
    void probeInfo() {
        File file = new File("/Users/lyl/Downloads/mp4-tmp/卫兰-大哥.mp3");
        MultimediaInfo multimediaInfo = ffmpegService.probeInfo(file.toPath());
        System.out.println(multimediaInfo);
    }

    @Test
    void getTmpFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        System.out.println("临时文件夹路径：" + tempDir);
    }
}