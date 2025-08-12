package github.nooblong.download.bilibili;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class PythonManager implements CommandLineRunner {

    @Value("${pythonCmd}")
    private String pythonCmd;
    @Value("${pythonPath}")
    private String pythonPath;
    @Value("${pythonPort}")
    private String pythonPort;

    private Process pythonProcess;
    private final Object lock = new Object();
    private long pid;

    public void start() throws IOException {
        synchronized (lock) {
            // 启动 Python 服务
            pythonProcess = new ProcessBuilder(pythonCmd, pythonPath, pythonPort)
                    .inheritIO() // 让输出直接显示到控制台
                    .start();
            pid = pythonProcess.pid();
            log.info("Python 服务已启动,pid={}", pid);
        }
    }

    public void stop() {
        synchronized (lock) {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                try {

                    String osName = System.getProperty("os.name").toLowerCase();
                    if (osName.contains("win")) {
                        Runtime.getRuntime().exec("taskkill /PID " + pid + " /T /F");
                    } else {
                        Runtime.getRuntime().exec("kill -TERM -" + pid);
                    }
                    log.info("Python 服务已停止,pid={}", pid);
                } catch (IOException e) {
                    log.error("关闭python服务失败", e);
                }
            }
            pythonProcess = null;
        }
    }

    public void restart() throws IOException {
        stop();
        start();
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Spring Boot 退出时，停止 Python 服务...");
        stop();
    }

    @Override
    public void run(String... args) throws Exception {
        this.start();
    }
}



