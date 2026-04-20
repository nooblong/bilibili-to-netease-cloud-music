package github.nooblong.btncm.service;

import java.nio.file.Path;

public interface FfmpegService {

    Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset, int bitrate);

}
