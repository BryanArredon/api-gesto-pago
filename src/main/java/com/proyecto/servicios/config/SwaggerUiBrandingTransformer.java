package com.proyecto.servicios.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Transformer de branding y Modo Oscuro para el Swagger UI de {@code api-gesto-pago}.
 *
 * <p>Inyecta {@code /favicon.png} y {@code /custom-swagger.css} en el {@code index.html}
 * de Swagger UI para aplicar el tema oscuro bancario homologado con {@code onboarding-clientes}.
 */
public class SwaggerUiBrandingTransformer implements SwaggerIndexTransformer {

    static final String HOJA_ESTILOS = "/custom-swagger.css";
    static final String SCRIPT_TEMA = "/custom-swagger.js";
    static final String FAVICON = "/favicon.png";

    private static final String PATRON_INDEX = "**/swagger-ui/**/index.html";

    private final SwaggerIndexTransformer delegate;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public SwaggerUiBrandingTransformer(SwaggerIndexTransformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Resource transform(
            HttpServletRequest request,
            Resource resource,
            ResourceTransformerChain chain)
            throws IOException {

        Resource transformado = delegate.transform(request, resource, chain);
        if (!esIndexDeSwaggerUi(transformado)) {
            return transformado;
        }

        String html = new String(transformado.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (html.contains(HOJA_ESTILOS)) {
            return transformado;
        }

        String branding = """
                <link rel="icon" type="image/png" href="%s" />
                <link rel="stylesheet" type="text/css" href="%s" />
                <script src="%s" defer></script>
                </head>""".formatted(FAVICON, HOJA_ESTILOS, SCRIPT_TEMA);
        String personalizado = html.replaceFirst("</head>", branding);

        return new TransformedResource(transformado, personalizado.getBytes(StandardCharsets.UTF_8));
    }

    private boolean esIndexDeSwaggerUi(Resource resource) {
        try {
            URL url = resource.getURL();
            return url != null && matcher.match(PATRON_INDEX, url.toString());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
