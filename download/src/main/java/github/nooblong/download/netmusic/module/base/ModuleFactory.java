package github.nooblong.download.netmusic.module.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Getter
@Component
@AllArgsConstructor
public class ModuleFactory implements ApplicationContextAware {


    private final Map<String, BaseModule> map;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, BaseModule> oldMap = applicationContext.getBeansOfType(BaseModule.class);
        for (Map.Entry<String, BaseModule> entry : oldMap.entrySet()) {
            String key = entry.getKey();
            BaseModule value = entry.getValue();
            map.put(key.toLowerCase(Locale.ROOT), value);
        }
    }

    public BaseModule getService(String key) {
        return map.get(key);
    }
}
