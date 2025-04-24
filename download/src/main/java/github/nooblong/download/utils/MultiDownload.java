package github.nooblong.download.utils;

import okhttp3.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class MultiDownload {
    private static final OkHttpClient client = new OkHttpClient();
    private static final int RETRY_LIMIT = 3;
    private static final int BUFFER_SIZE = 8192;

    public static void downloadWithRange(String url, String destPath, int partSize) throws IOException {
        long totalSize = fetchContentLength(url);
        if (totalSize <= 0) {
            throw new IOException("Unable to determine file size.");
        }

        System.out.println("Total size: " + totalSize + " bytes");

        try (FileChannel fileChannel = FileChannel.open(new File(destPath).toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            long start = 0;
            while (start < totalSize) {
                long end = Math.min(start + partSize - 1, totalSize - 1);
                byte[] data = fetchPart(url, start, end);
                if (data != null) {
                    fileChannel.position(start);
                    fileChannel.write(ByteBuffer.wrap(data));
                    System.out.println("downloaded part " + start + "-" + end);
                } else {
                    throw new IOException("Download failed for range: " + start + "-" + end);
                }
                start = end + 1;
            }
        }

        System.out.println("Download completed: " + destPath);
    }

    private static long fetchContentLength(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .addHeader("Referer", "https://bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 206) {
                String contentRange = response.header("Content-Range");
                if (contentRange != null) {
                    String totalSizeStr = contentRange.substring(contentRange.lastIndexOf("/") + 1);
                    return Long.parseLong(totalSizeStr);
                }
            } else if (response.code() == 200) {
                return response.body().contentLength();
            }
        }

        return -1;
    }

    private static byte[] fetchPart(String url, long start, long end) {
        int attempts = 0;
        while (attempts < RETRY_LIMIT) {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Range", "bytes=" + start + "-" + end)
                        .addHeader("Referer", "https://bilibili.com")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return response.body().bytes();
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to download range " + start + "-" + end + ", attempt " + (attempts + 1));
            }
            attempts++;
        }
        return null;
    }

    // 示例 main 方法
    public static void main(String[] args) {
        String fileUrl = "https://cn-hljheb-ct-01-02.bilivideo.com/upgcxcode/45/33/29412363345/29412363345-1-30251.m4s?e=ig8euxZM2rNcNbdlhoNvNC8BqJIzNbfqXBvEqxTEto8BTrNvN0GvT90W5JZMkX_YN0MvXg8gNEV4NC8xNEV4N03eN0B5tZlqNxTEto8BTrNvNeZVuJ10Kj_g2UB02J0mN0B5tZlqNCNEto8BTrNvNC7MTX502C8f2jmMQJ6mqF2fka1mqx6gqj0eN0B599M=&platform=pc&trid=00004afdb206b5674ed6be318ec2253d437u&og=hw&os=bcache&deadline=1745490276&oi=2030593090&tag=&nbs=1&uipk=5&mid=1486826767&gen=playurlv3&upsig=cdb4b1fe6a066165ded28f24a597b6d0&uparams=e,platform,trid,og,os,deadline,oi,tag,nbs,uipk,mid,gen&cdnid=3841&bvc=vod&nettype=0&bw=1599207&agrr=1&buvid=5FC28794-A9FC-C42F-162A-51D8EE07670676652infoc&build=0&dl=0&f=u_0_0&orderid=0,3";
        String destPath = "downloaded.flac";
        int partSize = 1024 * 512; // 1MB per part

        try {
            downloadWithRange(fileUrl, destPath, partSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
