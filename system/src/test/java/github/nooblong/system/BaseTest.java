package github.nooblong.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest(properties = {"spring.config.location=classpath:application-local.yml"})
public class BaseTest {

}
