package github.nooblong.download.bilibili;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

//@Service
public class PythonManager implements CommandLineRunner {

    @Value("${pythonCmd}")
    private String pythonCmd;
    @Value("${pythonPath}")
    private String pythonPath;
    @Value("${pythonPort}")
    private String pythonPort;

    private Process pythonProcess;
    private final Object lock = new Object();

    public void start() throws IOException {
        synchronized (lock) {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                System.out.println("Python 服务已在运行中");
                return;
            }
            // 启动 Python 服务
            pythonProcess = new ProcessBuilder(pythonCmd, pythonPath, pythonPort)
                    .inheritIO() // 让输出直接显示到控制台
                    .start();
            System.out.println("Python 服务已启动");
        }
    }

    public void stop() {
        synchronized (lock) {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                pythonProcess.destroyForcibly();
                System.out.println("Python 服务已停止");
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
        System.out.println("Spring Boot 退出时，停止 Python 服务...");
        stop();
    }

    @Override
    public void run(String... args) throws Exception {
        this.start();
    }
}



