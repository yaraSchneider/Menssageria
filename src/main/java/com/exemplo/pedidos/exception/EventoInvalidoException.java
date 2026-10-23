package com.exemplo.pedidos.exception;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Exceção lançada por um consumidor quando um {@code EventoPedido} recebido não
 * atende às regras de validação de conteúdo (cliente em branco, lista de itens
 * vazia ou valor total não positivo).
 *
 * <p>Estende {@link AmqpRejectAndDontRequeueException}, sinalizando ao Spring
 * AMQP que a mensagem é <strong>não recuperável</strong> e não deve ser
 * reenfileirada. Combinada com {@code default-requeue-rejected: false} e os
 * argumentos {@code x-dead-letter-*} das filas de processamento, a mensagem é
 * encaminhada automaticamente à Dead Letter Queue.</p>
 *
 * <p>Requisito atendido: 7.2.</p>
 */
public class EventoInvalidoException extends AmqpRejectAndDontRequeueException {

    public EventoInvalidoException(String message) {
        super(message);
    }

    public EventoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
