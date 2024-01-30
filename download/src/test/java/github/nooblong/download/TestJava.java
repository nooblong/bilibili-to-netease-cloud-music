package github.nooblong.download;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestJava {

    @Test
    public void testFfmpeg() throws IOException {
        FFmpeg fFmpeg = new FFmpeg("/Users/lyl/Documents/GitHub/nosync.nosync/little/oss/src/main/resources/tmpSound/ffmpeg");
        FFprobe fFprobe = new FFprobe("/Users/lyl/Documents/GitHub/nosync.nosync/little/oss/src/main/resources/tmpSound/ffprobe");

        String resource = "http://localhost:8888/services/files/download/anonymous-readwrite/%E3%80%90%E9%98%BF%E6%A2%93%E6%AD%8C%E3%80%91%E3%80%8A%E8%AE%A9%E9%A3%8E%E5%91%8A%E8%AF%89%E4%BD%A0%E3%80%8B%EF%BC%81%EF%BC%81%EF%BC%81vrc%E9%99%90%E5%AE%9A%E7%89%88%EF%BC%81-643739732";
        String output = "/Users/lyl/Documents/GitHub/nosync.nosync/little/oss/src/main/resources/filesystem-configs/anonymous-readwrite/output.mp3";
        FFmpegProbeResult probe = fFprobe.probe(resource);

//        System.out.println(probe.getFormat().duration);
        FFmpegStream x = probe.getStreams().get(0);
//        System.out.println(x);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(resource)
                .overrideOutputFiles(true)
                .addOutput(output)
                .setFormat("null")
                .setAudioBitRate(probe.getFormat().bit_rate)
                .setStartOffset(30, TimeUnit.SECONDS)
                .setDuration(60, TimeUnit.SECONDS)
                .done();

        FFmpegBuilder avg = new FFmpegBuilder()
                .setInput(resource)
                .setAudioFilter("volumedetect")
                .addStdoutOutput()
                .setFormat("null")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(fFmpeg, fFprobe);

        executor.createJob(avg, new ProgressListener() {
            final double duration_ns = probe.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

            @Override
            public void progress(Progress progress) {
                double percentage = progress.out_time_ns / duration_ns;
                System.out.printf(
                        "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx%n",
                        percentage * 100,
                        progress.status,
                        progress.frame,
                        FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                        progress.fps.doubleValue(),
                        progress.speed
                );
            }
        }).run();

        FFmpegJob fFmpegJob = executor.createJob(builder, new ProgressListener() {
            // Using the FFmpegProbeResult determine the duration of the input
            final double duration_ns = probe.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

            @Override
            public void progress(Progress progress) {
                try {
                    double percentage = progress.out_time_ns / duration_ns;
                    System.out.printf(
                            "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx%n",
                            percentage * 100,
                            progress.status,
                            progress.frame,
                            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                            progress.fps.doubleValue(),
                            progress.speed
                    );
                } catch (IllegalArgumentException e) {
                    System.out.println(e);
                }
            }
        });
        fFmpegJob.run();

        FFmpegFormat format = probe.getFormat();
//        System.out.format("%nFile: '%s' ; Format: '%s' ; Duration: %.3fs ; BitRate: %d",
//                format.filename,
//                format.format_long_name,
//                format.duration,
//                format.bit_rate
//        );

        FFmpegStream stream = probe.getStreams().get(0);
//        System.out.format("%nCodec: '%s' ; Width: %dpx ; Height: %dpx ; BitRate: %d",
//                stream.codec_long_name,
//                stream.width,
//                stream.height,
//                stream.bit_rate
//        );

//        FFmpegJob job = executor.createJob(builder);
//        job.run();
    }

    @Test
    public void testCharacters() {
        String str1 = "Hello, 【你好】！";
        String str2 = new String(str1.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        String str3 = "【】【】】【";
        System.out.println("str1 is valid UTF-8: " + isCharsetValid(str1, "UTF-8"));
        System.out.println("str2 is valid UTF-8: " + isCharsetValid(str2, "UTF-8"));
        System.out.println("str2 is valid ISO-8859-1: " + isCharsetValid(str2, "ISO-8859-1"));
        System.out.println("str3 is valid ASCII: " + isCharsetValid(str2, "ASCII"));
        System.out.println("str3 is valid ISO-8859-1: " + isCharsetValid(str2, "ISO-8859-1"));
    }

    public static boolean isCharsetValid(String str, String charsetName) {
        Charset charset = Charset.forName(charsetName);
        return charset.newEncoder().canEncode(str);
    }

    @Test
    public void testPattern() {
        String name1 = "【阿梓歌】《My Love》田馥甄（2022.7.16）";
        String name2 = "【阿梓】《白与黑》“只是风停了云散了寂寞，像只猫痛了后舔着伤口”";
        String name3 = "不许说510晚安-【阿梓】电台时期的可爱语录";
        String name4 = "【阿梓歌】《一路向北》！！！纯享版！";
        String name5 = "【小可學妹】《追光者》溫柔週末";
        List<String> list = Arrays.asList(name1, name2, name3, name4, name5);
        for (String s : list) {
            String r1 = "\\（(.*?)\\）";
            String s1 = ReUtil.extractMulti(r1, s, "$1");
            System.out.println(s1);
        }

        String str = "{1} {2}";

        String result = ReUtil.replaceAll(str, "\\{(.*?)}", match -> {
            String content = match.group(0);
            System.out.println(content);
            return "替换";
        });

        System.out.println("Result: " + result);

    }

    @Test
    public void testDate() {
        String name1 = "【阿梓歌】《My Love》田馥甄（2022.7.16）";
        String r1 = "\\（(.*?)\\）";
        String s1 = ReUtil.extractMulti(r1, name1, "$1");
        DateTime parse = DateUtil.parse(s1, "yyyy.MM.dd");
        String format = DateUtil.format(parse, "yyyy-MM-dd HH:mm:ss");
        System.out.println(parse.toLocalDateTime());
        System.out.println(format);
    }

}
