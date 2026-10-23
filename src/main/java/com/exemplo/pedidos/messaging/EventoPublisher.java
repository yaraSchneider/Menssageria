package com.exemplo.pedidos.messaging;

import com.exemplo.pedidos.model.EventoPedido;

/**
 * Produtor responsável por publicar eventos de pedido no RabbitMQ.
 *
 * <p>Abstrai a camada de mensageria para que a camada de serviço acione a
 * publicação sem conhecer detalhes do broker ou do formato de serialização.</p>
 *
 * <p>Requisitos atendidos: 2.1, 2.3.</p>
 */
public interface EventoPublisher {

    /**
     * Serializa o {@link EventoPedido} em JSON e o publica na Exchange_Topic de
     * pedidos com a routing key correspondente ao evento de criação de pedido.
     *
     * @param evento evento de pedido a ser publicado
     * @throws com.exemplo.pedidos.exception.PublicacaoEventoException se a
     *         publicação no broker falhar
     */
    void publicar(EventoPedido evento);
}
