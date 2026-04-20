package github.nooblong.btncm.service;

import java.nio.file.Path;

public interface FfmpegService {

    /**
     * 由于m4a格式在苹果设备会出问题，所以转码为mp3
     */
    Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset, int bitrate);

}
