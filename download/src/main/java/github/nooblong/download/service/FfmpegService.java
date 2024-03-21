package github.nooblong.download.service;

import ws.schild.jave.info.MultimediaInfo;

import java.nio.file.Path;

public interface FfmpegService {

    Path encodeMp3(Path sourceUrl, double beginSec, double endSec, double voiceOffset);

    MultimediaInfo probeInfo(Path sourceUrl);

}
