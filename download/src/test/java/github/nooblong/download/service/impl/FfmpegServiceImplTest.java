package github.nooblong.download.service.impl;

import github.nooblong.download.BaseTest;
import github.nooblong.download.service.FfmpegService;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;

class FfmpegServiceImplTest extends BaseTest {

    @Autowired
    FfmpegService ffmpegService;

    @Test
    void probeInfo() {
        String s = "/Users/lyl/Documents/GitHub/nosync.nosync/little/workingDir/oss/bilibili/";
        String s1 = "【阿梓歌】《一样的月光》徐佳莹（2022.5.17）-811739303-722822266.m4a";
        String s2 = ".mp3";

        FFmpegProbeResult fFmpegProbeResult = ffmpegService.probeInfo(Paths.get(s + s1));
        System.out.println(fFmpegProbeResult.getFormat().bit_rate / 1000);
        for (FFmpegStream stream : fFmpegProbeResult.getStreams()) {
            System.out.println(stream.bits_per_raw_sample);
        }
        System.out.println("-----");
        FFmpegProbeResult fFmpegProbeResult2 = ffmpegService.probeInfo(Paths.get(s + s1 + s2));
        System.out.println(fFmpegProbeResult2.getFormat().bit_rate / 1000);
        for (FFmpegStream stream : fFmpegProbeResult2.getStreams()) {
            System.out.println(stream.bits_per_raw_sample);
        }

    }
}