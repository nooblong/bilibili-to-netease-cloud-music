package github.nooblong.btncm.config;

import github.nooblong.btncm.netmusic.NetMusicForwardInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 让/direct的链接直接请求netmusic包的网易云接口，省去controller
 */
@Configuration
@AllArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final NetMusicForwardInterceptor netMusicForwardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(netMusicForwardInterceptor)
                .addPathPatterns("/direct/**")
        ;
    }

}
