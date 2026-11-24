package com.proyecto.servicios.config.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(prefix = "notificaciones.correo", name = "habilitado",
        havingValue = "true", matchIfMissing = true)
public class CorreoConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender notificacionJavaMailSender(
            @Value("${notificaciones.correo.smtp-host:}") String host,
            @Value("${notificaciones.correo.smtp-port:587}") int puerto,
            @Value("${notificaciones.correo.smtp-usuario:}") String usuario,
            @Value("${notificaciones.correo.smtp-password:}") String password,
            @Value("${notificaciones.correo.smtp-starttls:true}") boolean starttls,
            @Value("${notificaciones.correo.smtp-ssl:false}") boolean ssl) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(puerto);
        sender.setDefaultEncoding("UTF-8");
        if (StringUtils.hasText(usuario)) {
            sender.setUsername(usuario);
            sender.setPassword(password);
        }
        Properties propiedades = sender.getJavaMailProperties();
        propiedades.put("mail.transport.protocol", "smtp");
        propiedades.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(usuario)));
        propiedades.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        propiedades.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        propiedades.put("mail.smtp.connectiontimeout", "5000");
        propiedades.put("mail.smtp.timeout", "10000");
        propiedades.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
