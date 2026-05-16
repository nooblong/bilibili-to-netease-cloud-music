package github.nooblong.btncm.job;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import java.time.LocalDateTime;

public class TaskCollectAppender
        extends AppenderBase<ILoggingEvent> {

    private PatternLayout layout;

    @Override
    public void start() {

        layout = new PatternLayout();

        layout.setContext(getContext());

        layout.setPattern(
                "%d{yyyy-MM-dd HH:mm:ss.SSS} "
                        + "%-5level "
                        + "[%thread] "
                        + "%class.%method:%line "
                        + "- %msg%n"
        );

        layout.start();

        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {

        TaskContext ctx =
                TaskContextHolder.get();

        if (ctx == null) {
            return;
        }

        TaskLog log = new TaskLog();

        log.setTime(LocalDateTime.now());

        log.setLevel(
                event.getLevel().toString()
        );

        // 完整格式化日志
        log.setFormattedText(
                layout.doLayout(event)
        );

        // 异常
        IThrowableProxy proxy =
                event.getThrowableProxy();

        if (proxy != null) {

            log.setException(
                    ThrowableProxyUtil.asString(proxy)
            );
        }

        ctx.getLogs().add(log);
    }
}