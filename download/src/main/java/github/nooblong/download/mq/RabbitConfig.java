package github.nooblong.download.mq;

import github.nooblong.download.bilibili.BilibiliVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RabbitConfig {

    @Value("${queueTail}")
    private String queueTail;

    @Bean
    DirectExchange uploadDirect() {
        return ExchangeBuilder
                .directExchange("uploadDirect" + queueTail)
                .durable(true)
                .build();
    }

    @Bean
    FanoutExchange deadUploadFanout() {
        return new FanoutExchange("deadUploadFanout" + queueTail);
    }

    @Bean
    Queue uploadQueue() {
        return QueueBuilder.durable("uploadQueue" + queueTail)
                .withArgument("x-dead-letter-exchange", "deadUploadFanout" + queueTail)
                .withArgument("x-max-priority", 20)
                .build();
    }

    @Bean
    Queue deadUploadQueue() {
        return QueueBuilder.durable("deadUploadQueue" + queueTail).build();
    }

    @Bean
    Binding uploadBinding(DirectExchange exchange, @Qualifier("uploadQueue") Queue uploadQueue) {
        return BindingBuilder
                .bind(uploadQueue)
                .to(exchange)
                .with("uploadRouting");
    }

    @Bean
    Binding deadUploadBinding(FanoutExchange deadUploadFanout, @Qualifier("deadUploadQueue") Queue deadUploadQueue) {
        return BindingBuilder
                .bind(deadUploadQueue)
                .to(deadUploadFanout);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory jsonContainerFactory(
            Jackson2JsonMessageConverter jsonMessageConverter,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory simpleContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter(DefaultClassMapper defaultClassMapper) {
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
//        jackson2JsonMessageConverter.setClassMapper(defaultClassMapper);
        return jackson2JsonMessageConverter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("bilibiliVideo", BilibiliVideo.class);
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

}
