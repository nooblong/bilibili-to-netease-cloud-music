package github.nooblong.btncm.service.impl;

import cn.hutool.core.util.StrUtil;
import github.nooblong.btncm.service.FfmpegService;
import github.nooblong.btncm.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
            List<String> cmdList = new ArrayList<>();
            cmdList.add(locator.getExecutablePath());
            cmdList.add("-i");
            cmdList.add(source.getAbsolutePath());
            cmdList.add("-codec:a");
            cmdList.add("libmp3lame");
            cmdList.add("-q:a");
            cmdList.add("1");
            if (endSec != 0) {
                cmdList.add("-ss");
                cmdList.add(String.valueOf(beginSec));
                cmdList.add("-to");
                cmdList.add(String.valueOf(endSec));
            }
            if (voiceOffset != 0) {
                cmdList.add("-filter:a");
                cmdList.add("volume=" + voiceOffset);
            }
            cmdList.add(targetPath);

            String[] cmd = cmdList.toArray(new String[0]);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // 合并 stderr 到 stdout
            Process process = pb.start();
            // 异步读取输出，防止阻塞
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException e) {
                    log.error("ffmpeg错误", e);
                }
            }).start();

            boolean finished = process.waitFor(20, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
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
