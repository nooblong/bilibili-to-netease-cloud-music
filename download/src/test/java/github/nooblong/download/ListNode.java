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
import java.util.*;

class MyLinkedList {
    // 1->2->3->4->5
    Node head = new Node(-1, null, null);
    Node tail = new Node(-1, null, head);
    int size;

    public MyLinkedList() {

    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            return -1;
        }
        if (index < size / 2) {
            Node tmp = head.next;
            for (int i = 0; i < index; i++) {
                tmp = tmp.next;
            }
            return tmp.val;
        } else {
            Node tmp = tail.prev;
            for (int i = 0; i < size - index - 1; i++) {
                tmp = tmp.prev;
            }
            return tmp.val;
        }
    }

    public void addAtHead(int val) {
        if (size == 0) {
            Node node = new Node(val, tail, head);
            head.next = node;
            tail.prev = node;
        } else {
            Node node = new Node(val, head.next, head);
            head.next.prev = node;
            head.next = node;
        }
        size++;
    }

    public void addAtTail(int val) {
        if (size == 0) {
            Node node = new Node(val, tail, head);
            head.next = node;
            tail.prev = node;
        } else {
            Node node = new Node(val, tail, tail.prev);
            tail.prev.next = node;
            tail.prev = node;
        }
        size++;
    }

    public void addAtIndex(int index, int val) {
        if (index > size) {
            return;
        }
        if (index == size) {
            Node node = new Node(val, tail, tail.prev);
            tail.prev.next = node;
            tail.prev = node;
            size++;
            return;
        }
        // 1->2->  new->  3(tmp)->4->5
        if (index < size / 2) {
            Node tmp = head.next;
            for (int i = 0; i < index; i++) {
                tmp = tmp.next;
            }
            Node node = new Node(val, tmp, tmp.prev);
            tmp.prev.next = node;
            tmp.prev = node;
        } else {
            Node tmp = tail.prev;
            for (int i = 0; i < size - index - 1; i++) {
                tmp = tmp.prev;
            }
            Node node = new Node(val, tmp, tmp.prev);
            tmp.prev.next = node;
            tmp.prev = node;
        }
        size++;
    }

    public void deleteAtIndex(int index) {
        if (index > size - 1) {
            return;
        }
        // 1->2->3(tmp)->4->5
        if (index < size / 2) {
            Node tmp = head.next;
            for (int i = 0; i < index; i++) {
                tmp = tmp.next;
            }
            tmp.prev.next = tmp.next;
            tmp.next.prev = tmp.prev;
        } else {
            Node tmp = tail.prev;
            for (int i = 0; i < size - index - 1; i++) {
                tmp = tmp.prev;
            }
            tmp.prev.next = tmp.next;
            tmp.next.prev = tmp.prev;
        }
        size--;
    }

    static class Node {
        int val;
        Node next;
        Node prev;

        public Node(int val, Node next, Node prev) {
            this.val = val;
            this.next = next;
            this.prev = prev;
        }
    }

    @Test
    void swapNodesInPairs() {
        ListNode head = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        ListNode listNode = swapPairs(head);
        while (listNode != null) {
            System.out.println(listNode.val);
            listNode = listNode.next;
        }
    }

    public ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode a = new ListNode();
        ListNode b = new ListNode();
        ListNode result = new ListNode();
        int num = 0;
        boolean isA = true;
        ListNode tmpA = a;
        ListNode tmpB = b;
        while (head != null) {
            if (isA) {
                tmpA.next = head;
                isA = false;
                tmpA = tmpA.next;
            } else {
                tmpB.next = head;
                isA = true;
                tmpB = tmpB.next;
            }
            head = head.next;
            num++;
        }
        tmpA.next = null;
        tmpB.next = null;
        a = a.next;
        b = b.next;
        ListNode tmpResult = result;
        for (int i = 1; i < num + 1; i++) {
            if (i % 2 == 0) {
                if (a == null) {
                    tmpResult.next = b;
                    tmpResult = tmpResult.next;
                    break;
                } else {
                    tmpResult.next = a;
                    a = a.next;
                }
            } else {
                if (b == null) {
                    tmpResult.next = a;
                    tmpResult = tmpResult.next;
                    break;
                } else {
                    tmpResult.next = b;
                    b = b.next;
                }
            }

            tmpResult = tmpResult.next;
        }
        tmpResult.next = null;
        return result.next;
    }
}

public class ListNode {
    int val;
    ListNode next;

    ListNode() {
    }

    ListNode(int val) {
        this.val = val;
    }

    ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }
}


class TestJava {

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

    @Test
    public void leetcode5() {
        int[] ints = {2, 3, 3};
        int i = removeElement(ints, 3);
        for (int j = 0; j < i; j++) {
            System.out.print(ints[j] + " ");
        }
    }

    public int removeElement(int[] nums, int val) {
        int index = nums.length - 1;
        if (index == 0) {
            if (nums[0] == val) {
                return 0;
            } else {
                return 1;
            }
        }
        for (int i = 0; i < nums.length; i++) {
            if (i == index && nums[i] == val) {
                return index;
            }
            if (i == index && nums[i] != val) {
                return index + 1;
            }
            while (true) {
                if (index < 0) {
                    break;
                }
                if (index == i) {
                    if (nums[i] == val) {
                        return index;
                    } else {
                        return index + 1;
                    }
                }
                if (nums[i] == val) {
                    nums[i] = nums[index--];
                } else {
                    break;
                }
            }
            if (i == index) {
                return index + 1;
            }
        }
        return 0;
    }

    @Test
    public void leetcode6() {
//        System.out.println(minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));
        System.out.println(minSubArrayLen(15, new int[]{1, 2, 3, 4, 5}));
    }

    public int minSubArrayLen(int target, int[] nums) {
        int result = 9999999;
        int l = 0, r = 0;
        int sum = 0;
        while (r < nums.length) {
            sum += nums[r];
            if (sum >= target) {
                if (r - l < result) {
                    result = r - l + 1;
                }
                while (l < r) {
                    sum -= nums[l++];
                    if (sum >= target) {
                        if (r - l < result) {
                            result = r - l + 1;
                        }
                    } else {
                        break;
                    }
                }
            }
            r++;
        }
        if (l == 0 && r == nums.length && result == 9999999) {
            return 0;
        }
        return result;
    }

    @Test
    public void leetcode7() {
//        int[][] ints = generateMatrix(5);
        int[][] ints = generateMatrix2(5);
        for (int[] anInt : ints) {
            for (int i : anInt) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    public int[][] generateMatrix(int n) {
        int[][] s = new int[n][n];
        int x = 0, y = 0;
        s[0][0] = 1;
        for (int i = 1; i < n * n; i++) {
            // right no top
            if (x + 1 < n && s[y][x + 1] == 0 && (y == 0 || (s[y - 1][x + 1] != 0))) {
                s[y][x + 1] = i + 1;
                x++;
                continue;
            }
            // down no right
            if (y + 1 < n && s[y + 1][x] == 0 && (x == n - 1 || (s[y + 1][x + 1] != 0))) {
                s[y + 1][x] = i + 1;
                y++;
                continue;
            }
            // left no down
            if (x > 0 && s[y][x - 1] == 0 && (y == n - 1 || (s[y + 1][x - 1] != 0))) {
                s[y][x - 1] = i + 1;
                x--;
                continue;
            }
            // top no left
            if (y > 0 && s[y - 1][x] == 0 && (x == 0 || (s[y - 1][x - 1] != 0))) {
                s[y - 1][x] = i + 1;
                y--;
                continue;
            }
        }
        return s;
    }

    public int[][] generateMatrix2(int n) {
        int t = 0;
        int b = n - 1;
        int l = 0;
        int r = n - 1;
        int[][] s = new int[n][n];
        int k = 1;
        while (k <= n * n) {
            for (int i = l; i <= r; ++i, ++k) {
                s[t][i] = k;
            }
            ++t;
            for (int i = t; i <= b; ++i, ++k) {
                s[i][r] = k;
            }
            --r;
            for (int i = r; i >= l; --i, ++k) {
                s[b][i] = k;
            }
            --b;
            for (int i = b; i >= t; --i, ++k) {
                s[i][l] = k;
            }
            ++l;
        }
        return s;
    }

    @Test
    public void designLinkedList() {
        MyLinkedList obj = new MyLinkedList();
        obj.addAtHead(2);
        obj.deleteAtIndex(1);
        obj.addAtHead(2);
        obj.addAtHead(7);
        obj.addAtHead(3);
        obj.addAtHead(2);
        obj.addAtHead(5);
        obj.addAtTail(5);
        System.out.println(obj.get(5));
        for (int i = 0; i < obj.size; i++) {
            System.out.print(obj.get(i) + "->");
        }
        System.out.println();
        obj.addAtIndex(2, 99);
        for (int i = 0; i < obj.size; i++) {
            System.out.print(obj.get(i) + "->");
        }
        System.out.println();
        obj.addAtIndex(7, 100);
        for (int i = 0; i < obj.size; i++) {
            System.out.print(obj.get(i) + "->");
        }
        System.out.println();
        obj.deleteAtIndex(3);
        for (int i = 0; i < obj.size; i++) {
            System.out.print(obj.get(i) + "->");
        }
        System.out.println();

        System.out.println(obj.get(0));
        System.out.println(obj.get(1));
        System.out.println(obj.get(2));
        System.out.println(obj.get(3));
        System.out.println(obj.get(4));
        System.out.println(obj.get(5));
        System.out.println(obj.get(6));
        System.out.println(obj.get(7));
//        obj.addAtIndex(1, 1);
//        obj.deleteAtIndex(1);

    }

    @Test
    public void removeNthNodeFromEndOfList() {
        ListNode listNode = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        ListNode listNode1 = removeNthFromEnd(listNode, 2);
        ListNode listNode2 = removeNthFromEnd(new ListNode(1), 1);
        printListNode(listNode1);
    }

    public ListNode removeNthFromEnd(ListNode head, int n) {
        if (head == null) {
            return null;
        }
        ListNode node = head;
        Map<Integer, ListNode> map = new HashMap<>();
        int index = 0;
        while (node != null) {
            map.put(++index, node);
            node = node.next;
        }
        int find = index - n + 1;
        if (find == 1) {
            return head.next;
        }
        if (find == index) {
            map.get(find - 1).next = null;
            return head;
        }
        map.get(find - 1).next = map.get(find + 1);
        return head;
    }


    public void printListNode(ListNode head) {
        while (head != null) {
            System.out.print(head.val + " ");
            head = head.next;
        }
        System.out.println();
    }

    public <T> void printListList(List<List<T>> lists) {
        for (List<T> list : lists) {
            for (T i : list) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    @Test
    void recorderList() {
        ListNode listNode = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        ListNode listNode2 = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4))));
        reorderList(listNode2);
        printListNode(listNode2);
    }

    public void reorderList(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode mid = slow;
        ListNode slow2 = slow;
        ListNode prev = null;
        ListNode tail = null;
        while (slow2 != null) {
            ListNode temp = slow2.next;
            slow2.next = prev;
            prev = slow2;
            slow2 = temp;
            tail = prev;
        }
        printListNode(tail);
        printListNode(head);
        ListNode copy = head;
        while (tail != null && tail != mid) {
            ListNode tmp = tail.next;
            tail.next = copy.next;
            copy.next = tail;
            tail = tmp;
            copy = copy.next.next;
        }
    }

    @Test
    public void findCommonCharacters() {
        List<String> strings = commonChars(new String[]{"bella", "label", "roller"});
        List<String> strings2 = commonChars(new String[]{"acabcddd", "bcbdbcbd", "baddbadb", "cbdddcac",
                "aacbcccd", "ccccddda", "cababaab", "addcaccd"});
        System.out.println(strings);
        System.out.println(strings2);
    }

    public List<String> commonChars(String[] words) {
        int[] map = new int[26];
        char[] as = words[0].toCharArray();
        for (char a : as) {
            map[a - 'a'] += 1;
        }
        for (int i = 1; i < words.length; i++) {
            char[] bs = words[i].toCharArray();
            int[] map2 = new int[26];
            for (char b : bs) {
                if (map[b - 'a'] > 0) {
                    map2[b - 'a'] += 1;
                }
            }
            for (int j = 0; j < map2.length; j++) {
                if (map2[j] == 0 && map[j] > 0) {
                    map[j] = 0;
                    continue;
                }
                map[j] = Math.min(map[j], map2[j]);
            }
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            if (map[i] > 0) {
                for (int k = 0; k < map[i]; k++) {
                    result.add((char) (i + 'a') + "");
                }
            }
        }
        return result;
    }

    @Test
    public void threeSum1() {
//        List<List<Integer>> lists = threeSum(new int[]{-1, 0, 1, 2, -1, -4});
//        List<List<Integer>> lists = threeSum(new int[]{0, 0, 0, 0});
//        List<List<Integer>> lists = threeSum(new int[]{-2, 0, 1, 1, 2});
        List<List<Integer>> lists = threeSum(new int[]{1, 2, -2, -1});
        for (List<Integer> list : lists) {
            for (Integer i : list) {
                System.out.print(i + " ");
            }
            System.out.println();
        }
    }

    public List<List<Integer>> threeSum(int[] nums) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        int b = nums.length / 3 * 2;
        for (int i = 0; i < b; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (j == b) {
                    continue;
                }
                int sum = nums[i] + nums[j];
                List<Integer> orDefault = map.getOrDefault(sum, new ArrayList<>());
                orDefault.addAll(Arrays.asList(i, j));
                map.put(sum, orDefault);
            }
        }
        List<List<Integer>> result = new ArrayList<>();
        List<List<Integer>> added = new ArrayList<>();
        for (int k = b; k < nums.length; k++) {
            int need = -nums[k];
            if (map.containsKey(need)) {
                List<Integer> tmp = map.get(need);
                for (int i = 0; i < tmp.size() / 2; i++) {
                    ArrayList<Integer> toAdd = new ArrayList<>(Arrays.asList(
                            nums[tmp.get(i * 2)], nums[tmp.get(i * 2 + 1)], nums[k]));
                    boolean isAdded = false;
                    for (List<Integer> tmp2 : added) {
                        if (tmp2.containsAll(toAdd)) {
                            isAdded = true;
                            break;
                        }
                    }
                    added.add(toAdd);
                    if (!isAdded) {
                        result.add(toAdd);
                    }
                }
            }
        }
        return result;
    }

    @Test
    void fourSum() {
//        int[] nums = {-2, -1, 0, 0, 1, 2};
//        int[] nums = {2, 2, 2, 2};
        int[] nums = {0, 0, 0, 1000000000, 1000000000, 1000000000, 1000000000};
//        List<List<Integer>> lists = fourSum(nums, 0);
//        List<List<Integer>> lists = fourSum(nums, 8);
        List<List<Integer>> lists = fourSum(nums, 1000000000);
        printListList(lists);
    }

    public List<List<Integer>> fourSum(int[] nums, int target) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;
        for (int a = 0; a < n - 3; a++) {
            if (a > 0 && nums[a] == nums[a - 1]) {
                continue;
            }
            long tmp1 = (long) nums[a] + (long) nums[n - 1] + (long) nums[n - 2] + (long) nums[n - 3];
            long tmp2 = (long) nums[a] + (long) nums[a + 1] + (long) nums[a + 2] + (long) nums[a + 3];
            if (tmp1 < target) {
                continue;
            }
            if (tmp2 > target) {
                break;
            }
            for (int b = a + 1; b < n - 2; b++) {
                // b>a+1而不是b>0处理2,2,2,2;8
                if (b > a + 1 && nums[b] == nums[b - 1]) {
                    continue;
                }
                long tmp3 = (long) nums[a] + (long) nums[b] + (long) nums[n - 1] + (long) nums[n - 2];// a固定
                long tmp4 = (long) nums[a] + (long) nums[b] + (long) nums[b + 1] + (long) nums[b + 2];// a固定
                if (tmp3 < target) {
                    continue;
                }
                if (tmp4 > target) {
                    break;
                }
                int c = b + 1, d = n - 1;
                while (c < d) {
                    long s = (long) nums[a] + (long) nums[b] + (long) nums[c] + (long) nums[d];
                    if (s == target) {
                        result.add(new ArrayList<>(Arrays.asList(nums[a], nums[b], nums[c], nums[d])));
                        do {
                            c++;
                        } while (nums[c] == nums[c - 1] && c < d);
                        do {
                            d--;
                        } while (nums[d] == nums[d + 1] && c < d);
                    } else if (s < target) {
                        do {
                            c++;
                        } while (nums[c] == nums[c - 1] && c < d);
                    } else {
                        do {
                            d--;
                        } while (nums[d] == nums[d + 1] && c < d);
                    }
                }
            }
        }
        return result;
    }

    @Test
    void findTheIndexOfTheFirstOccurrenceInAString() {
        System.out.println(strStr("sahdbutsad", "sad"));
        System.out.println(strStr("aabaabaafa", "aabaaf"));
    }

    public int strStr(String haystack, String needle) {
        int[] next = new int[needle.length()];
        int j = 0;
        next[0] = 0;
        for (int i = 1; i < needle.length(); i++) {
            while (j > 0 && needle.charAt(j) != needle.charAt(i)) {
                j = next[j - 1];
            }
            if (needle.charAt(i) == needle.charAt(j)) {
                j++;
            }
            next[i] = j;
        }
//        System.out.println(Arrays.toString(next));
        int k = 0;
        for (int i = 0; i < haystack.length(); i++) {
            while (k > 0 && haystack.charAt(i) != needle.charAt(k)) {
                k = next[k - 1];
            }
            if (haystack.charAt(i) == needle.charAt(k)) {
                k++;
            }
            if (k == needle.length()) {
                return i - needle.length() + 1;
            }
        }
        return 0;
    }

    @Test
    void repeatedSubstringPattern() {
//        System.out.println(repeatedSubstringPattern("abab"));
//        System.out.println(repeatedSubstringPattern("aba"));
//        System.out.println(repeatedSubstringPattern("abcabcabcabc"));
//        System.out.println(repeatedSubstringPattern("aabaabassss"));
        System.out.println(repeatedSubstringPattern("abaababaab"));
//        System.out.println(repeatedSubstringPattern("ab"));
    }

    public boolean repeatedSubstringPattern(String s) {
        char[] c = s.toCharArray();
        char[] cycle = new char[0];
        if (s.length() == 1) {
            return false;
        }
        for (int i = 1; i < c.length; i++) {
            if (i > c.length / 2) {
                return false;
            }
            if (c[0] == c[i]) {
                cycle = Arrays.copyOfRange(c, 0, i);
                break;
            }
        }
        if (cycle.length == 0) {
            return false;
        }
        int index = 0;
        int start = cycle.length;
        for (int i = start; i < c.length; i++) {
            if (cycle[index] == c[i]) {
                index++;
                if (index == cycle.length) {
                    index = 0;
                }
            } else {
                for (int j = cycle.length + 1; j < c.length; j++) {
                    if (c[j] == c[0] && c[j - 1] == c[c.length - 1]) {
                        cycle = Arrays.copyOfRange(c, 0, j);
                        i = cycle.length - 1;
                        index = 0;
                        break;
                    }
                    if (j > c.length / 2 + 1) {
                        return false;
                    }
                }
            }
        }
        return index == 0;
    }

}

