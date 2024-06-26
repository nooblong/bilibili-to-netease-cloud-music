package github.nooblong.download;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"github.nooblong.*"})
@MapperScan(basePackages = {"github.nooblong.*.mapper"})
@EnableScheduling
@EnableAsync
@Slf4j
public class Download {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(Download.class, args);
    }
}
