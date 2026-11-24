package com.proyecto.servicios.config.notificacion;

import com.proyecto.servicios.config.security.JwtService;
import com.proyecto.servicios.service.security.VerificadorSesionOnboarding;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class NotificacionWebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final VerificadorSesionOnboarding verificadorSesion;

    public NotificacionWebSocketAuthInterceptor(
            JwtService jwtService,
            VerificadorSesionOnboarding verificadorSesion) {
        this.jwtService = jwtService;
        this.verificadorSesion = verificadorSesion;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            autenticar(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validarSuscripcion(accessor);
        }
        return message;
    }

    private void autenticar(StompHeaderAccessor accessor) {
        String token = tokenDe(accessor);
        if (!StringUtils.hasText(token)) {
            throw new MessagingException("Falta el token de autorizacion en el CONNECT");
        }
        try {
            Claims claims = jwtService.validarAccessToken(token);
            if (jwtService.esDeOnboarding(claims)
                    && !verificadorSesion.esVigente(
                            claims.get(JwtService.CLAIM_SESION, String.class), token)) {
                throw new MessagingException("La sesion del token ya no esta vigente");
            }
            String email = jwtService.extraerEmail(claims);
            List<SimpleGrantedAuthority> autoridades = jwtService.extraerRoles(claims).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, autoridades));
        } catch (JwtException e) {
            throw new MessagingException("Token JWT invalido: " + e.getMessage());
        }
    }

    private void validarSuscripcion(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        if (!StringUtils.hasText(destino)) {
            return;
        }
        if (destino.startsWith("/queue/")
                || (destino.startsWith("/user/") && !destino.startsWith("/user/queue/"))) {
            throw new MessagingException("Suscripcion no permitida: " + destino);
        }
    }

    private String tokenDe(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(header)) {
            header = accessor.getFirstNativeHeader("authorization");
        }
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER)) {
            return null;
        }
        return header.substring(BEARER.length()).trim();
    }
}
