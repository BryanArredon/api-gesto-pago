package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoProductoClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoListaResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoProductoClient gestoPagoProductoClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoProductoClient gestoPagoProductoClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.gestoPagoProductoClient = gestoPagoProductoClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }

    @Override
    public List<GestoPagoProducto> obtenerProductos() {
        GestoPagoProductoListaResponse response = consultarProductos();
        if (!esExitoso(response)) {
            log.warn("GestoPago no devolvió productos (codigo={}), renovando token y reintentando",
                    response == null || response.getMensaje() == null ? null : response.getMensaje().getCodigo());
            renovarToken();
            response = consultarProductos();
        }
        if (!esExitoso(response)) {
            String texto = response == null || response.getMensaje() == null
                    ? "sin mensaje" : response.getMensaje().getTexto();
            throw new IllegalStateException("GestoPago no devolvió la lista de productos: " + texto);
        }
        List<GestoPagoProducto> productos = response.getProductos() == null
                ? null : response.getProductos().getProducto();
        return productos == null ? List.of() : productos;
    }

    private GestoPagoProductoListaResponse consultarProductos() {
        GestoPagoToken token = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .orElseThrow(() -> new IllegalStateException("No hay token GestoPago guardado"));
        String xml = gestoPagoProductoClient.getProductList("Bearer " + token.getToken());
        try {
            JAXBContext context = JAXBContext.newInstance(GestoPagoProductoListaResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (GestoPagoProductoListaResponse) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new IllegalStateException("Error al interpretar la respuesta de GestoPago", e);
        }
    }

    private boolean esExitoso(GestoPagoProductoListaResponse response) {
        return response != null
                && response.getMensaje() != null
                && "01".equals(response.getMensaje().getCodigo());
    }
}
