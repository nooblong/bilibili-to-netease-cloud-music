package download.service.impl;

import download.BaseTest;
import github.nooblong.download.service.FfmpegService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
class FfmpegServiceImplTest extends BaseTest {

    @Autowired
    FfmpegService ffmpegService;

    @Test
    void getTmpFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tempDir, "btncm");
        try {
            Path newDir = Files.createDirectory(path);
            log.info("文件夹创建成功：" + newDir);
        } catch (FileAlreadyExistsException e) {
            log.info("文件夹已经存在: " + path);
        } catch (IOException e) {
            log.error("无法创建文件夹: " + path);
            throw new RuntimeException(e);
        }
    }
}