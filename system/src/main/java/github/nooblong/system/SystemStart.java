package github.nooblong.system;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"github.nooblong.common", "github.nooblong.download"})
@MapperScan(basePackages = {"github.nooblong.common.mapper", "github.nooblong.download.mapper"})
@EnableCaching
@Slf4j
public class SystemStart {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(SystemStart.class, args);
    }
}
