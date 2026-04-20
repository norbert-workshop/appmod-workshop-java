package com.microsoft.migration.assets.worker.config;

import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String IMAGE_PROCESSING_QUEUE = "image-processing";

    @Bean
    public Queue imageProcessingQueue() {
        return QueueBuilder.durable(IMAGE_PROCESSING_QUEUE)
        .build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // CWE-502: Restrict deserialization to known safe types only.
        // Explicitly map type IDs to concrete classes; no arbitrary class from __TypeId__ header is accepted.
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("imageProcessingMessage", ImageProcessingMessage.class);
        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("com.microsoft.migration.assets");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

}
