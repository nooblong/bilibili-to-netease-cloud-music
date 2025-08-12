package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FfmpegServiceImpl implements FfmpegService, InitializingBean {

    @Override
    public Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset, int bitrate) {
        Assert.isTrue(Files.exists(sourceUrl), "转码失败，" + sourceUrl + "不是文件");
        File source = sourceUrl.toFile();
        String targetPath = sourceUrl.toAbsolutePath() + "." + Constant.FFMPEG_FORMAT_MP3;
        try {
            DefaultFFMPEGLocator locator = new DefaultFFMPEGLocator();
            String cmd = locator.getExecutablePath()
                    + " -i " + source.getAbsolutePath()
                    + " -codec:a libmp3lame -q:a 1 "
                    + (endSec != 0 ? " -ss " + beginSec + " -to " + endSec : "")
                    + (voiceOffset != 0 ? " -filter:a \"volume=" + voiceOffset + "\" " : "")
                    + targetPath;
            log.info("ffmpeg命令: {}", cmd);
            Runtime runtime = Runtime.getRuntime();
            Process exec = runtime.exec(cmd);
            boolean b = exec.waitFor(60, TimeUnit.SECONDS);
            if (!b) {
                throw new RuntimeException("转码超时");
            }
        } catch (Exception e) {
            throw new RuntimeException("转码失败: " + e.getMessage());
        }
        log.info("转码完成: {}", sourceUrl);
        return Paths.get(targetPath);
    }


    @Override
    public void afterPropertiesSet() {
        Assert.isTrue(StrUtil.isNotBlank(Constant.TMP_FOLDER),
                "工作目录为空");
    }
}
