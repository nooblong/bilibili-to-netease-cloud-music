package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class FfmpegServiceImpl implements FfmpegService, InitializingBean {

    @Override
    public Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset, int bitrate) {
        Assert.isTrue(Files.exists(sourceUrl), "转码失败，" + sourceUrl + "不是文件");
        File source = sourceUrl.toFile();
        String targetPath = sourceUrl.toAbsolutePath() + "." + Constant.FFMPEG_FORMAT_MP3;
        File target = new File(targetPath);
        EncodingAttributes encodingAttributes = getEncodingAttributes(beginSec, endSec, (int) voiceOffset,
                Constant.FFMPEG_FORMAT_MP3, bitrate);
        Encoder encoder = new Encoder();
        try {
            encoder.encode(new MultimediaObject(source), target, encodingAttributes);
        } catch (EncoderException e) {
            log.error("转码失败: ", e);
            throw new RuntimeException(e);
        }
        log.info("转码: {}", sourceUrl);
        return Paths.get(targetPath);
    }

    @NotNull
    public static EncodingAttributes getEncodingAttributes(Double beginSec, Double endSec, Integer voiceOffset,
                                                           String ext, int bitrate) {
        AudioAttributes audioAttributes = new AudioAttributes();
        if (voiceOffset != null && voiceOffset != 0) {
            audioAttributes.setVolume(voiceOffset);
        }
        audioAttributes.setCodec("libmp3lame");
        if (bitrate == 0) {
            bitrate = 320000;
        }
        audioAttributes.setBitRate(bitrate);
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setAudioAttributes(audioAttributes);
        encodingAttributes.setOutputFormat(ext);
        if (beginSec != null && endSec != null && endSec != 0) {
            double duration = endSec - beginSec;
            encodingAttributes.setDuration((float) duration);
            encodingAttributes.setOffset(beginSec.floatValue());
        }

        VideoAttributes videoAttributes = new VideoAttributes();
        encodingAttributes.setVideoAttributes(videoAttributes);
        return encodingAttributes;
    }

    @Override
    public MultimediaInfo probeInfo(Path sourceUrl) {
        MultimediaObject multimediaObject = new MultimediaObject(sourceUrl.toFile());
        try {
            return multimediaObject.getInfo();
        } catch (EncoderException e) {
            log.error("查看音频信息错误: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        Assert.isTrue(StrUtil.isNotBlank(Constant.TMP_FOLDER),
                "工作目录为空");
    }
}
