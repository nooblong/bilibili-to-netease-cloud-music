package github.nooblong.btncm.config;

import github.nooblong.btncm.netmusic.NetMusicForwardInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
