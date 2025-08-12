package github.nooblong.download.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

@Slf4j
public class MultiDownload {
    private static final OkHttpClient client = new OkHttpClient();
    private static final int RETRY_LIMIT = 3;
    // 1024 * 512: 1MB per part

    public static void downloadWithRange(String url, File destFile, int partSize,
                                         String referer) throws IOException {
        long totalSize = fetchContentLength(url, referer);
        if (totalSize <= 0) {
            log.error("获取到负数文件大小");
            throw new IOException("Unable to determine file size.");
        }
        log.info("文件总大小: {}", totalSize);

        try (FileChannel fileChannel = FileChannel.open(destFile.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            long start = 0;
            while (start < totalSize) {
                long end = Math.min(start + partSize - 1, totalSize - 1);
                byte[] data = fetchPart(url, start, end, referer);
                if (data != null) {
                    fileChannel.position(start);
                    fileChannel.write(ByteBuffer.wrap(data));
                    log.info("下载 {}-{}", start, end);
                } else {
                    log.error("下载分区数据为空 {}-{}", start, end);
                    throw new IOException("下载分区数据为空: " + start + "-" + end);
                }
                start = end + 1;
            }
        }

        log.info("所有分区下载完成: {}", destFile.getAbsolutePath());
    }

    private static long fetchContentLength(String url, String referer) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .addHeader("Referer", referer)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 206) {
                String contentRange = response.header("Content-Range");
                if (contentRange != null) {
                    String totalSizeStr = contentRange.substring(contentRange.lastIndexOf("/") + 1);
                    return Long.parseLong(totalSizeStr);
                }
            } else if (response.code() == 200) {
                assert response.body() != null;
                return response.body().contentLength();
            }
        }

        return -1;
    }

    private static byte[] fetchPart(String url, long start, long end, String referer) {
        int attempts = 0;
        while (attempts < RETRY_LIMIT) {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Range", "bytes=" + start + "-" + end)
                        .addHeader("Referer", referer)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().bytes();
                    }
                }
            } catch (IOException e) {
                log.error("分区下载失败 {}-{}, 重试次数 {}", start, end, attempts + 1);
            }
            attempts++;
        }
        return null;
    }

}
