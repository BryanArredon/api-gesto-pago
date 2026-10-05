package com.onboarding.clientes.shared.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cargador de variables de entorno desde archivo .env local o en el directorio superior.
 */
public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {}

    public static void cargarSiExiste() {
        String userDir = System.getProperty("user.dir");
        // Intentar en el directorio actual y en el directorio padre
        cargarDesde(userDir);
        cargarDesde(Path.of(userDir).getParent() != null ? Path.of(userDir).getParent().toString() : null);
    }

    public static void cargarDesde(String directorio) {
        if (directorio == null || directorio.isBlank()) {
            return;
        }
        Path ruta = Path.of(directorio, ".env");
        if (!Files.exists(ruta)) {
            return;
        }
        Map<String, String> variables = parsear(ruta);
        int cargadas = 0;
        for (Map.Entry<String, String> entrada : variables.entrySet()) {
            String clave = entrada.getKey();
            if (System.getProperty(clave) != null || System.getenv(clave) != null) {
                continue;
            }
            System.setProperty(clave, entrada.getValue());
            cargadas++;
        }
        if (cargadas > 0) {
            log.info("Cargado .env desde {} ({} variables inyectadas)", ruta.toAbsolutePath(), cargadas);
        }
    }

    static Map<String, String> parsear(Path ruta) {
        Map<String, String> resultado = new LinkedHashMap<>();
        try {
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                String limpia = linea.trim();
                int indiceIgual = limpia.indexOf('=');
                if (!limpia.isEmpty() && !limpia.startsWith("#") && indiceIgual >= 1) {
                    String clave = limpia.substring(0, indiceIgual).trim();
                    String valor = limpia.substring(indiceIgual + 1).trim();
                    if (valor.length() >= 2
                            && ((valor.startsWith("\"") && valor.endsWith("\""))
                            || (valor.startsWith("'") && valor.endsWith("'")))) {
                        valor = valor.substring(1, valor.length() - 1);
                    }
                    if (!clave.isBlank()) {
                        resultado.put(clave, valor);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo leer .env en {}: {}", ruta.toAbsolutePath(), e.getMessage());
        }
        return resultado;
    }
}
