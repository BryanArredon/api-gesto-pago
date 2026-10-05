package com.onboarding.clientes.shared.evento;

/**
 * Puerto de publicacion de eventos de dominio.
 *
 * <p>La implementacion por defecto los persiste como registro de auditoria. La interfaz permite sustituirla
 * por un publicador a una cola de mensajes sin tocar los casos de uso.
 */
public interface PublicadorEvento {

    void publicar(EventoDominio evento);
}
