package github.nooblong.btncm.bilibili;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * python控制器，用于通过 Docker 管理 bilibili-api 服务
 */
@Service
@Slf4j
public class PythonManager {

    @Value("${pythonDockerService:bilibili-api}")
    private String pythonDockerService;

    private final Object lock = new Object();

    public void restart() throws IOException {
        synchronized (lock) {
            runDockerCommand("restart", pythonDockerService);
            log.info("bilibili-api Docker 服务已重启: {}", pythonDockerService);
        }
    }

    private void runDockerCommand(String... args) throws IOException {
        String[] command = new String[args.length + 1];
        command[0] = "docker";
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            int exitCode = new ProcessBuilder(command)
                    .inheritIO()
                    .start()
                    .waitFor();
            if (exitCode != 0) {
                throw new IOException("Docker 命令执行失败: " + Arrays.toString(command) + ", exitCode=" + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Docker 命令被中断: " + Arrays.toString(command), e);
        }
    }
}


