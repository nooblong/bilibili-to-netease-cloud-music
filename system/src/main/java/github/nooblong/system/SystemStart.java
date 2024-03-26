package github.nooblong.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"github.nooblong.*"})
//@MapperScan(basePackages = {"github.nooblong.*.mapper"})
@Slf4j
public class SystemStart {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(SystemStart.class, args);
    }
}
