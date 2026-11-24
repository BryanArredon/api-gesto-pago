package com.proyecto.servicios.config.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final NotificacionWebSocketAuthInterceptor authInterceptor;
    private final String endpoint;
    private final String allowedOrigins;

    public WebSocketConfig(
            NotificacionWebSocketAuthInterceptor authInterceptor,
            @Value("${notificaciones.ws.endpoint:/ws}") String endpoint,
            @Value("${notificaciones.ws.allowed-origins:${security.cors.allowed-origins}}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.endpoint = endpoint;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origenes = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toArray(String[]::new);
        var registration = registry.addEndpoint(endpoint);
        if (origenes.length == 0) {
            registration.setAllowedOriginPatterns("*");
        } else {
            registration.setAllowedOriginPatterns(origenes);
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
