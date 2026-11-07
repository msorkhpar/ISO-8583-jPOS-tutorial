package com.example.jpos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wires a bean from a configuration class, as the tutorial's client setup does. */
class SpringContextTest {

    @Configuration
    static class Wiring {
        @Bean
        String channelName() {
            return "client-channel";
        }
    }

    @Test
    void aConfigurationClassProvidesItsBean() {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(Wiring.class)) {
            assertEquals("client-channel", context.getBean("channelName", String.class));
        }
    }
}
