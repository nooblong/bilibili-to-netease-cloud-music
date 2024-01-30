package github.nooblong.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok(String message) {
        return new Result<T>().setMessage(message).setCode(0);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<T>().setMessage(message).setCode(0).setData(data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<T>().setMessage(message).setCode(-1);
    }

    public static <T> Result<T> okOrNot(Integer code, String message, T data, boolean ok) {
        if (!ok) {
            return new Result<T>().setMessage(message).setCode(code).setData(data);
        }
        return new Result<T>().setMessage(message).setCode(code);
    }
}
