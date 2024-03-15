//package github.nooblong.download.mq;
//
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.core.MessagePostProcessor;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.Assert;
//
//@Component
//public class MessageSender {
//
//    private final RabbitTemplate rabbitTemplate;
//    final DirectExchange directExchange;
//
//    @Autowired
//    public MessageSender(RabbitTemplate rabbitTemplate, DirectExchange directExchange) {
//        this.rabbitTemplate = rabbitTemplate;
//        this.directExchange = directExchange;
//    }
//
//    public void sendUploadDetailId(Long id, int priority) {
//        Assert.notNull(id, "发送消息为空id");
//        MessagePostProcessor messagePostProcessor = message -> {
//            message.getMessageProperties().setPriority(priority);
//            return message;
//        };
//        rabbitTemplate.convertAndSend(directExchange.getName(), "uploadRouting", id, messagePostProcessor);
//    }
//
//    public void sendMessageCustom(Object o, String exchange, String routingKey) {
//        rabbitTemplate.convertAndSend(exchange, routingKey, o);
//    }
//
//}
