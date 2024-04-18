package github.nooblong.download;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import com.fasterxml.jackson.databind.JsonNode;
import github.nooblong.download.utils.OkUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TestJava {

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

    @Test
    void testFfmpeg2() throws EncoderException {
        File source = new File("/Users/lyl/Documents/GitHub/nosync.nosync/bilibili-to-netease-cloud-music-private/workingDir/download/[null]-[【阿梓】不知道起什么标题总之就是可爱~]-[BV1HK41187ud]-[null].m4a");
        System.out.println(source.exists());
        File target = new File("/Users/lyl/Documents/GitHub/nosync.nosync/bilibili-to-netease-cloud-music-private/workingDir/download/target.mp3");

        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setBitRate(320000);

        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setAudioAttributes(audioAttributes);
        encodingAttributes.setOutputFormat("mp3");

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, encodingAttributes);
    }

    @Test
    void testJob() {
        PowerJobClient powerJobClient = new PowerJobClient("127.0.0.1:7700", "****", "****");
        ResultDTO<JobInfoDTO> jobInfoDTOResultDTO = powerJobClient.fetchJob(1L);
        System.out.println(jobInfoDTOResultDTO);
        ResultDTO<Integer> integerResultDTO = powerJobClient.fetchInstanceStatus(650633601515257920L);
        ResultDTO<InstanceInfoDTO> instanceInfoDTOResultDTO = powerJobClient.fetchInstanceInfo(650633601515257920L);
        System.out.println(integerResultDTO);
        System.out.println(instanceInfoDTOResultDTO);
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

    @Test
    void leetcode1() {
        int[] nums1 = {1, 3, 4, 9};
        int[] nums2 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] nums3 = {1, 2};
        int[] nums4 = {3, 4};
        int[] nums5 = {};
        int[] nums6 = {2, 3};
        int[] nums7 = {2};
        int[] nums8 = {1, 3, 4};
        int[] nums9 = {1};
        int[] nums10 = {2, 3, 4, 5, 6};
        int[] nums11 = {1, 4};
        int[] nums12 = {2, 3, 5, 6};
        System.out.println(findMedianSortedArrays(nums1, nums2));
        System.out.println(findMedianSortedArrays(nums3, nums4));
        System.out.println(findMedianSortedArrays(nums5, nums6));
        System.out.println(findMedianSortedArrays(nums7, nums8));
        System.out.println(findMedianSortedArrays(nums9, nums10));
        System.out.println(findMedianSortedArrays(nums11, nums12));
    }

    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int len1 = nums1.length;
        int len2 = nums2.length;
        if ((len1 + len2) % 2 != 0) {
            int k = (len1 + len2) / 2 + 1;
            return findK(k, nums1, nums2);
        } else {
            int k = (len1 + len2) / 2;
            return (findK(k, nums1, nums2) + findK(k + 1, nums1, nums2)) / 2d;
        }
    }

    public double findK(int k, int[] nums1, int[] nums2) {
        int index1 = 0, index2 = 0;
        for (; ; ) {
            if (nums1.length - index1 == 0 || nums2.length - index2 == 0) {
                return nums1.length - index1 == 0 ? nums2[k + index2 - 1] : nums1[k + index1 - 1];
            }
            if (k == 1) {
                return Math.min(nums1[index1], nums2[index2]);
            }
            int index = (k / 2) - 1;
            int specialReduce = 0;
            if (nums1.length - index1 <= index) {
                index = nums1.length - 1;
                specialReduce = nums1.length - index1;
            }
            if (nums2.length - index2 <= index) {
                index = nums2.length - 1;
                specialReduce = nums2.length - index2;
            }
            if (nums1[index1 + index] <= nums2[index2 + index]) {
                index1 += index + 1;
                k = specialReduce == 0 ? k - k / 2 : k - specialReduce;
            } else {
                index2 += index + 1;
                k = specialReduce == 0 ? k - k / 2 : k - specialReduce;
            }
        }
    }

    @Test
    void leetcode2() {
        System.out.println(longestPalindrome("babad"));
        System.out.println(longestPalindrome("a"));
        System.out.println(longestPalindrome("ac"));
        System.out.println(longestPalindrome("bb"));
        System.out.println(longestPalindrome("ccc"));
        System.out.println(longestPalindrome("ccd"));
        System.out.println(longestPalindrome("aaaa"));
        System.out.println(longestPalindrome("aacabdkacaa"));
    }

    public String longestPalindrome(String s) {
        if (s.length() <= 1) {
            return s;
        }
        String max1 = "";
        if (s.length() == 2) {
            if (s.charAt(0) == s.charAt(1)) {
                return s;
            }
        }
        for (int i = 0; i < s.length(); i++) {
            // i=0为从字符0后面分割
            String max2 = "";
            for (int j = 0; j < Math.min(i, s.length() - i - 1); j++) {
                if (s.charAt(i - 1 - j) == s.charAt(i + 1 + j)) {
                    // -1 、 +1
                    String substring = s.substring(i - j - 1, i + 1 + j + 1);
                    if (substring.length() > max2.length()) {
                        max2 = substring;
                    }
                } else {
                    break;
                }
            }
            if (i < s.length() - 1 && s.charAt(i) == s.charAt(i + 1)) {
                String substring = s.substring(i, i + 1 + 1);
                if (substring.length() > max2.length()) {
                    max2 = substring;
                }
            }
            for (int j = 0; j < Math.min(i + 1, s.length() - i - 1); j++) {
                if (s.charAt(i - j) == s.charAt(i + 1 + j)) {
                    String substring = s.substring(i - j, i + 1 + j + 1);
                    if (substring.length() > max2.length()) {
                        max2 = substring;
                    }
                } else {
                    break;
                }
            }
            if (max1.length() < max2.length()) {
                max1 = max2;
            }
        }
        if (max1.isEmpty()) {
            return String.valueOf(s.charAt(0));
        }
        return max1;
    }

    @Test
    void leetcode3() {
//        System.out.println(convert("PAYPALISHIRING", 4));
//        System.out.println(convert("ABCDE", 4));
        System.out.println(convert("PAYPALISHIRING", 5));
    }

    public String convert(String s, int numRows) {
        char[] ca = s.toCharArray();
        char[] result = new char[s.length()];
        if (numRows == 1 || numRows >= s.length()) {
            return s;
        }
        int groupSize = numRows + numRows - 2;
        int groupNum = s.length() / groupSize;
        int lastGroupSize = s.length() % groupSize;
        int index = 0;
        for (int row = 0; row < numRows; row++) {
            int lastLineCharNum = 0;
            if (lastGroupSize <= numRows) {
                if (row + 1 <= lastGroupSize) {
                    lastLineCharNum++;
                }
            } else {
                lastLineCharNum++;
                if (row != numRows - 1) {
                    if (numRows - (row + 1) <= lastGroupSize - numRows) {
                        lastLineCharNum++;
                    }
                }

            }

            if (row == 0 || row == numRows - 1) {
                for (int j = 0; j < groupNum + (lastLineCharNum != 0 ? 1 : 0); j++) {
                    int charAt = row + j * groupSize;
                    result[index] = ca[charAt];
                    index++;
                }
            } else {
                for (int j = 0; j < groupNum + (lastLineCharNum != 0 ? 1 : 0); j++) {
                    if (lastLineCharNum > 0 && j == groupNum + lastLineCharNum - 1) {
                        int charAt1 = row + j * groupSize;
                        result[index] = ca[charAt1];
                        index++;
                        if (lastLineCharNum > 1) {
                            int numBetween = (numRows - (row + 1)) * 2 - 1;
                            int charAt2 = row + j * groupSize + numBetween + 1;
                            result[index] = ca[charAt2];
                        }
                        break;
                    }
                    int charAt1 = row + j * groupSize;
                    result[index] = ca[charAt1];
                    index++;
                    int numBetween = (numRows - (row + 1)) * 2 - 1;
                    int charAt2 = row + j * groupSize + numBetween + 1;
                    result[index] = ca[charAt2];
                    index++;
                }
            }
        }
        return new String(result);
    }

    @Test
    void leetcode4() {
        System.out.println(reverse(1534236469));
        System.out.println(reverse(1463847412));
        System.out.println(reverse(1563847412));
//        System.out.println(reverse(2147483651));
    }

    public int reverse(int x) {
        int rev = 0;
        while (x != 0) {
            if (rev < Integer.MIN_VALUE / 10 || rev > Integer.MAX_VALUE / 10) {
                return 0;
            }
            int digit = x % 10;
            x /= 10;
            rev = rev * 10 + digit;
        }
        return rev;
    }

}
