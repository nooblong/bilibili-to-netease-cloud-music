package github.nooblong.download.service.impl;

import cn.hutool.core.util.StrUtil;
import github.nooblong.download.service.FfmpegService;
import github.nooblong.download.utils.Constant;
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
    public Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset) {
        Assert.isTrue(Files.exists(sourceUrl), "转码失败，" + sourceUrl + "不是文件");
        File source = sourceUrl.toFile();
        String targetPath = sourceUrl.toAbsolutePath() + "." + Constant.FFMPEG_FORMAT_MP3;
        File target = new File(targetPath);
        EncodingAttributes encodingAttributes = getEncodingAttributes(beginSec, endSec, (int) voiceOffset, Constant.FFMPEG_FORMAT_MP3);
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
    public static EncodingAttributes getEncodingAttributes(double beginSec, double endSec, int voiceOffset, String ext) {
        double duration = endSec - beginSec;

        AudioAttributes audioAttributes = new AudioAttributes();
        // 设置编码过程的音量值。如果为 null 或未指定，则将选择默认值。如果是 256，则不会执行任何音量更改。
        // 音量是“振幅比”或“声压级”比率 2560 是音量=20dB 公式是 dBnumber=20*lg(振幅比) 128 表示减小 50% 512 表示音量加倍
        audioAttributes.setVolume(voiceOffset);
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setAudioAttributes(audioAttributes);
        encodingAttributes.setOutputFormat(ext);
        encodingAttributes.setDuration((float) duration);
        encodingAttributes.setOffset((float) beginSec);

        VideoAttributes videoAttributes = new VideoAttributes();
        encodingAttributes.setVideoAttributes(videoAttributes);
        return encodingAttributes;
    }

    @Override
    public MultimediaInfo probeInfo(Path sourceUrl) {
        MultimediaObject multimediaObject = new MultimediaObject(sourceUrl.toFile());
        try {
            MultimediaInfo info = multimediaObject.getInfo();
            log.info("format: {}", info.getFormat());
            return info;
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
