package github.nooblong.download.config;

import cn.hutool.core.exceptions.ValidateException;
import github.nooblong.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class MvcControllerAdvice {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<String>> handleException(RuntimeException e) {
        log.error("未捕捉错误", e);
        Result<String> fail = Result.fail(e.getMessage());
        return new ResponseEntity<>(fail, HttpStatusCode.valueOf(500));
    }

    @ExceptionHandler(ValidateException.class)
    public ResponseEntity<Result<String>> handleException(ValidateException e) {
        log.error(e.getMessage());
        Result<String> fail = Result.fail("未登录");
        return new ResponseEntity<>(fail, HttpStatusCode.valueOf(403));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<String>> handleException(DataAccessException e) {
        log.error(e.getMessage());
        Result<String> fail = Result.fail("数据库错误");
        return new ResponseEntity<>(fail, HttpStatusCode.valueOf(500));
    }

}
