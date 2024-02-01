package github.nooblong.download.service;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;

import java.nio.file.Path;

public interface FfmpegService {

    Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset);

    FFmpegProbeResult probeInfo(Path sourceUrl);

}
