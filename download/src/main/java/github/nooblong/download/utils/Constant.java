package github.nooblong.download.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Constant {
    public static final String BILI_API_URL = "localhost";
    public static final int BILI_API_PORT = 9000;
    public static final String FULL_BILI_API = "http://" + BILI_API_URL + ":" + BILI_API_PORT;
    public static final String BILI_DOWNLOAD_FOLDER = "download";
    public static final String FFMPEG_FORMAT_MP3 = "mp3";
    public static final String FFMPEG_FORMAT_FLAC = "flac";
    public static final String FFMPEG_FORMAT_M4A = "m4a";
    public static final int SEARCH_PAGE_SIZE = 30;
    public static final int MAX_RETRY_TIMES = 5;
    public static String TMP_FOLDER;

    static {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tempDir, "btncm");
        try {
            Path newDir = Files.createDirectory(path);
            log.info("文件夹创建成功：" + newDir);
        } catch (FileAlreadyExistsException e) {
            log.info("文件夹已经存在: " + path);
        } catch (IOException e) {
            log.error("无法创建文件夹: " + path);
            throw new RuntimeException(e);
        }
        TMP_FOLDER = path.toAbsolutePath().toString();
    }
}
