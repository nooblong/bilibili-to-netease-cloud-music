package github.nooblong.download.config;

import github.nooblong.download.netmusic.NetMusicForwardInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@AllArgsConstructor
public class MvcConfig extends WebMvcConfigurationSupport {

    private final NetMusicForwardInterceptor netMusicForwardInterceptor;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(netMusicForwardInterceptor)
//                .addPathPatterns("/direct/login/qr/check")
//                .addPathPatterns("/direct/login/status")
//                .addPathPatterns("/direct/voice/list/search")
//                .addPathPatterns("/direct/dj/program/detail")
//                .addPathPatterns("/direct/dj/program/detail")
//                .addPathPatterns("/direct/dj/detail")
                .addPathPatterns("/direct/**")
        ;
    }

    /**
     * 假装为静态文件
     *
     * @param registry 注册器
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/resource/", "classpath:/static/");
        super.addResourceHandlers(registry);
    }
}
