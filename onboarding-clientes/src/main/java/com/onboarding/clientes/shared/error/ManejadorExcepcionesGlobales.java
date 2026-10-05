package com.onboarding.clientes.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo global y centralizado de errores.
 *
 * <p>Reglas de seguridad aplicadas aqui (OWASP A10 - Mishandling of Exceptional Conditions):
 *
 * <ul>
 *   <li>El cliente nunca recibe trazas de pila, nombres de tabla, sentencias SQL ni rutas del sistema de
 *       archivos: los errores inesperados se registran en el log con su identificador de traza y se
 *       responde con un mensaje generico.
 *   <li>Los errores de negocio se traducen a su codigo estable, sin filtrar existencia de datos mas alla
 *       de lo que el propio recurso implica.
 *   <li>Los errores de base de datos por restriccion se traducen al error de dominio equivalente, de modo
 *       que la carrera entre dos peticiones concurrentes produce el mismo 409 que la validacion previa.
 * </ul>
 */
@RestControllerAdvice
public class ManejadorExcepcionesGlobales {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorExcepcionesGlobales.class);

    @ExceptionHandler(ErrorNegocio.class)
    public ResponseEntity<ProblemDetail> manejarErrorNegocio(ErrorNegocio error, HttpServletRequest peticion) {
        ProblemDetail problem =
                FabricaProblemas.crear(error.codigoError(), error.codigoError().codigo(), error.getMessage(), ruta(peticion));
        if (!error.detalles().isEmpty()) {
            problem.setProperty("detalles", error.detalles());
        }
        LOGGER.info("Error de negocio: codigo={} traza={}", error.codigoError().codigo(), TrazaActual.obtener());
        return ResponseEntity.status(error.codigoError().estado()).body(problem);
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<ProblemDetail> manejarDatoInvalido(DatoInvalidoException error, HttpServletRequest peticion) {
        ProblemDetail problem =
                FabricaProblemas.crear(error.codigoError(), "Dato invalido", error.getMessage(), ruta(peticion));
        FabricaProblemas.conErroresDeCampo(
                problem, error.problemas().stream()
                        .map(mensaje -> new FabricaProblemas.ViolacionCampo("dato", mensaje))
                        .toList());
        return ResponseEntity.status(error.codigoError().estado()).body(problem);
    }

    /** Validacion del cuerpo de la peticion ({@code @Valid} sobre el DTO de entrada). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacionCuerpo(
            MethodArgumentNotValidException error, HttpServletRequest peticion) {
        List<FabricaProblemas.ViolacionCampo> errores = error.getBindingResult().getFieldErrors().stream()
                .map(this::traducir)
                .toList();
        return responderValidacion(errores, ruta(peticion));
    }

    /** Validacion de parametros sueltos y de tipos ({@code @Validated} en el controlador). */
    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ProblemDetail> manejarValidacionParametros(Exception error, HttpServletRequest peticion) {
        List<FabricaProblemas.ViolacionCampo> errores = List.of();
        if (error instanceof ConstraintViolationException excepcion) {
            errores = excepcion.getConstraintViolations().stream()
                    .map(violacion -> new FabricaProblemas.ViolacionCampo(
                            rutaCampo(violacion), mensajeAmigable(violacion.getMessage())))
                    .toList();
        }
        return responderValidacion(errores, ruta(peticion));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> manejarCuerpoIlegible(
            HttpMessageNotReadableException error, HttpServletRequest peticion) {
        LOGGER.debug("Cuerpo de la peticion ilegible: {}", error.getMessage());
        return responder(CodigoError.SOLICITUD_INVALIDA, "El cuerpo de la peticion no es un JSON valido", ruta(peticion), null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> manejarParametroInvalido(Exception error, HttpServletRequest peticion) {
        return responder(CodigoError.SOLICITUD_INVALIDA, "Parametro de la peticion invalido", ruta(peticion), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException error, HttpServletRequest peticion) {
        return responder(CodigoError.METODO_NO_PERMITIDO, "Metodo HTTP no soportado para este recurso", ruta(peticion), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> manejarRecursoNoEncontrado(
            NoResourceFoundException error, HttpServletRequest peticion) {
        return responder(CodigoError.RECURSO_NO_ENCONTRADO, "El recurso solicitado no existe", ruta(peticion), null);
    }

    /**
     * Traduce una violacion de integridad de la base a un error de dominio. Es la ultima linea de defensa
     * contra duplicados: si dos peticiones concurrentes[email protected] al mismo tiempo, la base rechaza
     * a una de ellas y la respuesta es el mismo 409 que produce la validacion previa.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> manejarIntegridad(
            DataIntegrityViolationException error, HttpServletRequest peticion) {
        CodigoError codigo = resolverCodigoPorRestriccion(error);
        if (codigo == CodigoError.ERROR_INTERNO) {
            LOGGER.warn("Violacion de integridad no clasificada: {}", error.getMostSpecificCause().getMessage());
        }
        return responder(codigo, mensajePorCodigo(codigo), ruta(peticion), null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail>manejarConflictoConcurrente(
            OptimisticLockingFailureException error, HttpServletRequest peticion) {
        return responder(
                CodigoError.CONFLICTO_CONCURRENTE,
                "El recurso fue modificado por otra transaccion; reintente la operacion",
                ruta(peticion),
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException error, HttpServletRequest peticion) {
        return responder(CodigoError.ACCESO_DENEGADO, "No cuenta con permisos para esta operacion", ruta(peticion), null);
    }

    /** Ultima linea: nunca se filtra informacion interna al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarInesperado(Exception error, HttpServletRequest peticion) {
        String traza = TrazaActual.obtener();
        LOGGER.error("Error inesperado traza={} ruta={}", traza, ruta(peticion), error);
        ProblemDetail problem = FabricaProblemas.crear(
                CodigoError.ERROR_INTERNO,
                "Error interno",
                "Ocurrio un error inesperado. El equipo de soporte ya fue notificado con la traza indicada.",
                ruta(peticion));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ResponseEntity<ProblemDetail> responderValidacion(
            List<FabricaProblemas.ViolacionCampo> errores, String ruta) {
        ProblemDetail problem = FabricaProblemas.crear(
                CodigoError.SOLICITUD_INVALIDA,
                "Solicitud invalida",
                "La peticion contiene datos que no cumplen las reglas de validacion",
                ruta);
        FabricaProblemas.conErroresDeCampo(problem, errores);
        return ResponseEntity.badRequest().body(problem);
    }

    private ResponseEntity<ProblemDetail> responder(
            CodigoError codigo, String mensaje, String ruta, List<FabricaProblemas.ViolacionCampo> errores) {
        ProblemDetail problem = FabricaProblemas.crear(codigo, mensajePorCodigo(codigo), mensaje, ruta);
        FabricaProblemas.conErroresDeCampo(problem, errores);
        return ResponseEntity.status(codigo.estado()).body(problem);
    }

    private FabricaProblemas.ViolacionCampo traducir(FieldError error) {
        return new FabricaProblemas.ViolacionCampo(error.getField(), mensajeAmigable(error.getDefaultMessage()));
    }

    private static String rutaCampo(ConstraintViolation<?> violacion) {
        String ruta = violacion.getPropertyPath().toString();
        int indice = ruta.lastIndexOf('.');
        return indice >= 0 ? ruta.substring(indice + 1) : ruta;
    }

    /** Sustituye los textos tecnicos de Bean Validation por mensajes en espanol comprensibles. */
    private static String mensajeAmigable(String mensaje) {
        if (mensaje == null) {
            return "valor invalido";
        }
        return switch (mensaje) {
            case "must not be null" -> "es obligatorio";
            case "must not be blank" -> "no puede estar vacio";
            case "must not be empty" -> "no puede estar vacio";
            case "must be positive" -> "debe ser mayor que cero";
            case "must be greater than 0" -> "debe ser mayor que cero";
            case "must be positive or zero" -> "debe ser mayor o igual a cero";
            case "must be greater than or equal to 0" -> "debe ser mayor o igual a cero";
            case "must be less than or equal to 18" -> "no puede exceder 18 caracteres";
            case "must be less than or equal to 50" -> "no puede exceder 50 caracteres";
            case "must be less than or equal to 100" -> "no puede exceder 100 caracteres";
            case "must be a past date" -> "no puede ser una fecha futura";
            case "must be greater than or equal to 2" -> "debe tener al menos 2 caracteres";
            default -> mensaje.toLowerCase(Locale.ROOT);
        };
    }

    private static String ruta(HttpServletRequest peticion) {
        return peticion.getRequestURI();
    }

    /**
     * Mapea el nombre de la restriccion de PostgreSQL al error de dominio equivalente. Solo se inspecciona
     * el nombre de la restriccion (no el valor de los datos) para no filtrar informacion.
     */
    private static CodigoError resolverCodigoPorRestriccion(DataIntegrityViolationException error) {
        String mensaje = mensajeCausa(error);
        if (mensaje.contains("uq_cliente_curp")) {
            return CodigoError.CURP_DUPLICADA;
        }
        if (mensaje.contains("uq_cliente_rfc")) {
            return CodigoError.RFC_DUPLICADO;
        }
        if (mensaje.contains("ux_cliente_correo_lower")) {
            return CodigoError.CORREO_DUPLICADO;
        }
        if (mensaje.contains("uq_cuenta_numero")) {
            return CodigoError.NUMERO_CUENTA_INVALIDO;
        }
        if (mensaje.contains("ux_usuario_correo_lower")) {
            return CodigoError.CORREO_DUPLICADO_USUARIO;
        }
        if (mensaje.contains("uq_usuario_cliente")) {
            return CodigoError.CLIENTE_YA_REGISTRADO;
        }
        if (mensaje.contains("ck_cuenta_saldo_no_negativo")) {
            return CodigoError.SALDO_INVALIDO;
        }
        return CodigoError.ERROR_INTERNO;
    }

    private static String mensajeCausa(Throwable error) {
        Throwable causa = error;
        while (causa != null) {
            if (causa.getMessage() != null) {
                return causa.getMessage();
            }
            causa = causa.getCause();
        }
        return "";
    }

    private static String mensajePorCodigo(CodigoError codigo) {
        return switch (codigo) {
            case CURP_DUPLICADA -> "Ya existe un cliente registrado con esa CURP";
            case RFC_DUPLICADO -> "Ya existe un cliente registrado con ese RFC";
            case CORREO_DUPLICADO, CORREO_DUPLICADO_USUARIO ->
                "Ya existe un cliente registrado con ese correo electronico";
            case CLIENTE_YA_REGISTRADO -> "El cliente ya tiene un usuario asociado";
            case NUMERO_CUENTA_INVALIDO -> "No fue posible generar un numero de cuenta unico";
            case SALDO_INVALIDO -> "El saldo no puede ser negativo";
            default -> codigo.codigo();
        };
    }
}
