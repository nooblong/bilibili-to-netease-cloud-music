package github.nooblong.download.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringPage {
    /**
     * 当前页数
     */
    private long index;
    /**
     * 总页数
     */
    private long totalPages;
    /**
     * 文本数据
     */
    private String data;

    public static StringPage simple(String data) {
        StringPage sp = new StringPage();
        sp.index = 0;
        sp.totalPages = 1;
        sp.data = data;
        return sp;
    }
}
