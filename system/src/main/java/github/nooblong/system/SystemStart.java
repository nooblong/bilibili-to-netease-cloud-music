package github.nooblong.system;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"github.nooblong.*"})
@MapperScan(basePackages = {"github.nooblong.common.mapper", "github.nooblong.download.mapper"})
@EnableScheduling
@Slf4j
public class SystemStart {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(SystemStart.class, args);
    }
}
