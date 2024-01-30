package github.nooblong.download.service.impl;

import cn.hutool.core.util.NumberUtil;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.utils.Constant;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FfmpegServiceImpl implements FfmpegService, InitializingBean {

    @Value("${workingDir}")
    private String workingDir;

    FFmpeg fFmpeg;
    FFprobe fFprobe;

    @Override
    public Path encodeMp3Cracked(Path sourceUrl, double beginSec, double endSec, double voiceOffset) {
        return encodeMp3(sourceUrl, beginSec, endSec, voiceOffset);
    }

    @Override
    public Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset) {
        Assert.isTrue(Files.exists(sourceUrl), "转码失败，" + sourceUrl + "不是文件");
        boolean cutLength = beginSec != 0 && endSec != 0;
        double duration = endSec - beginSec;

        FFmpegBuilder builder = new FFmpegBuilder();
        long millStartOffset = Math.round(beginSec * 1000);
        long millDuration = Math.round(duration * 1000);
        String targetPath = sourceUrl.toAbsolutePath() + "." + Constant.FFMPEG_FORMAT_MP3;
        if (cutLength) {
            builder
                    .setInput(sourceUrl.toAbsolutePath().toString())
                    .overrideOutputFiles(true)
                    .addOutput(targetPath)
                    .setFormat(Constant.FFMPEG_FORMAT_MP3)
                    .setAudioBitRate(320000)
                    .setStartOffset(millStartOffset, TimeUnit.MILLISECONDS)
                    .setDuration(millDuration, TimeUnit.MILLISECONDS)
                    .setAudioFilter("volume=" + NumberUtil.round(voiceOffset, 2) + "dB")
                    .done();
        } else {
            builder
                    .setInput(sourceUrl.toAbsolutePath().toString())
                    .overrideOutputFiles(true)
                    .addOutput(targetPath)
                    .setFormat(Constant.FFMPEG_FORMAT_MP3)
                    .setAudioBitRate(320000)
                    .setAudioFilter("volume=" + NumberUtil.round(voiceOffset, 2) + "dB")
                    .done();
        }

        FFmpegExecutor executor = new FFmpegExecutor(fFmpeg, fFprobe);
        FFmpegJob job = executor.createJob(builder);
        log.info("转码: {}", sourceUrl);
        job.run();
        return Paths.get(targetPath);
    }

    @Override
    public FFmpegProbeResult probeInfo(Path sourceUrl) {
        FFmpegProbeResult probe;
        try {
            probe = fFprobe.probe(sourceUrl.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return probe;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(workingDir, "ffmpeg文件夹未找到");
        try {
            fFmpeg = new FFmpeg(Paths.get(workingDir, "ffmpeg").toString());
            fFprobe = new FFprobe(Paths.get(workingDir, "ffprobe").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
