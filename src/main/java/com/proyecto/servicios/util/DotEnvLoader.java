package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void cargarSiExiste() {
        cargarDesde(System.getProperty("user.dir"));
    }

    public static void cargarDesde(String directorio) {
        if (directorio == null || directorio.isBlank()) {
            return;
        }
        Path ruta = Path.of(directorio, ".env");
        if (!Files.exists(ruta)) {
            log.debug("No se encontro .env en {}; se usan variables de entorno del sistema", directorio);
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
        log.info("Cargado .env desde {} ({} variables inyectadas como propiedades de sistema)",
                ruta.toAbsolutePath(), cargadas);
    }

    static Map<String, String> parsear(Path ruta) {
        Map<String, String> resultado = new LinkedHashMap<>();
        try {
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                String limpia = linea.trim();
                if (limpia.isEmpty() || limpia.startsWith("#")) {
                    continue;
                }
                int indiceIgual = limpia.indexOf('=');
                if (indiceIgual < 1) {
                    continue;
                }
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
        } catch (IOException e) {
            log.warn("No se pudo leer .env en {}: {}", ruta.toAbsolutePath(), e.getMessage());
        }
        return resultado;
    }
}