package com.onboarding.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Punto de entrada de la API de onboarding de clientes personas fisicas.
 *
 * <p>La aplicacion se organiza por funcionalidad ({@code cliente}, {@code auth}) en lugar de por capa tecnica. Cada
 * funcionalidad contiene sus capas {@code domain}, {@code application}, {@code infrastructure} y {@code api}; las
 * reglas de dependencia entre capas se verifican con ArchUnit (ver {@code ArquitecturaTest}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@EnableJpaAuditing
public class OnboardingApplication {

    public static void main(String[] args) {
        com.onboarding.clientes.shared.config.DotEnvLoader.cargarSiExiste();
        SpringApplication.run(OnboardingApplication.class, args);
    }
}
