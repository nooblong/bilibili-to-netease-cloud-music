package download;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.common.util.CommonUtil;
import github.nooblong.download.utils.OkUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class JavaTest {

    @Test
    public void testPatternNew() {
//        String title = "【阿梓歌】《My Love》田馥甄（2022.7.16）";
        String title = "【小可學妹】《熬夜上癮》 溫柔嬌嫩{title}{title}";
//        String regexName = "【阿梓歌{{【(.*?)】}}】{{(田馥甄}},{{（.*?】}}《My Love{{《(.*?)》}}》田馥甄（2022.7.16）{{（(.*?)）}}";
        String regexName = "2021.1.1 {{《(.*?)》}}";
        String r1 = "\\{\\{(.*?)\\}\\}";
        List<String> all = ReUtil.findAll(r1, regexName, 1);
        all.forEach(System.out::println);
        for (String s : all) {
            try {
                String s1 = ReUtil.extractMulti(s, title, "$1");
                regexName = regexName.replaceAll(ReUtil.escape("{{" + s + "}}"),
                        Objects.requireNonNullElse(s1, "无匹配结果"));
            } catch (Exception e) {
                regexName = regexName.replaceAll(ReUtil.escape("{{" + s + "}}"), "正则有误");
            }
        }
        System.out.println(regexName);
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
    void time() throws InterruptedException {
        String time = "03:30";
        LocalTime time1 = LocalTime.parse(time);
        int minute = time1.getMinute();
        int second = time1.getSecond();
        System.out.println(minute);
        System.out.println(second);
        Thread.sleep(2000);
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
    public void testDate() {
        String name1 = "【阿梓歌】《My Love》田馥甄（2022.7.16）";
        String r1 = "\\（(.*?)\\）";
        String s1 = ReUtil.extractMulti(r1, name1, "$1");
        DateTime parse = DateUtil.parse(s1, "yyyy.MM.dd");
        String format = DateUtil.format(parse, "yyyy-MM-dd HH:mm:ss");
        System.out.println(parse.toLocalDateTime());
        System.out.println(format);
    }

    @Test
    void testFfmpeg2() throws EncoderException {
        File source = new File("/Users/lyl/Documents/GitHub/nosync" +
                ".nosync/bilibili-to-netease-cloud-music-private/workingDir/download/[null" +
                "]-[【阿梓】不知道起什么标题总之就是可爱~]-[BV1HK41187ud]-[null].m4a");
        System.out.println(source.exists());
        File target = new File("/Users/lyl/Documents/GitHub/nosync" +
                ".nosync/bilibili-to-netease-cloud-music-private/workingDir/download/target.mp3");

        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setBitRate(320000);

        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setAudioAttributes(audioAttributes);
        encodingAttributes.setOutputFormat("mp3");

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, encodingAttributes);
    }

    @Test
    void testFfmpeg3() throws EncoderException {
        File source = new File("/Users/lyl/Downloads/test.m4a");
        System.out.println(source.exists());
        File target = new File("/Users/lyl/Downloads/target.mp3");

        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setCodec("libmp3lame");
        audioAttributes.setBitRate(320000);

        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setAudioAttributes(audioAttributes);
        encodingAttributes.setOutputFormat("mp3");

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, encodingAttributes);
    }

    @Test
    void testFfmpeg4() throws EncoderException {
        CommonUtil.debug();
        File source = new File("/Users/lyl/Downloads/test.m4a");
        MultimediaObject multimediaObject = new MultimediaObject(source);
        try {
            MultimediaInfo info = multimediaObject.getInfo();
            System.out.println(info.getAudio().getBitRate());
            System.out.println(info.getFormat());
        } catch (EncoderException e) {
            System.out.println("查看音频信息错误: " + e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testApi() {
        OkHttpClient okHttpClient = new OkHttpClient();
        String url = "http://127.0.0.1:7700/system/listWorker?appId=1";
        Request request = OkUtil.get(url);
        JsonNode jsonResponse = OkUtil.getJsonResponse(request, okHttpClient);
        System.out.println(jsonResponse.toPrettyString());
    }

    @Test
    void testSubstring() {
        String substring = "bili_jct=9dc72e5c01a78c51569778830e0b7767".substring(9);
        System.out.println(substring);
    }

}
