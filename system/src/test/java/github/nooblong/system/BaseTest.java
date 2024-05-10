package github.nooblong.system;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest(properties = {"spring.config.location=classpath:application-local.yml"})
public class BaseTest {

}
