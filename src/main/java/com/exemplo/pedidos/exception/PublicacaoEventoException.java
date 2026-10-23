package com.exemplo.pedidos.exception;

/**
 * Exceção lançada quando a publicação de um evento de pedido no RabbitMQ falha.
 *
 * <p>É uma {@link RuntimeException} para não poluir a assinatura dos métodos de
 * publicação. O {@code GlobalExceptionHandler} a traduz para o status HTTP 502,
 * indicando que a Pedido_API não conseguiu encaminhar o evento ao broker.</p>
 *
 * <p>Requisito atendido: 2.4.</p>
 */
public class PublicacaoEventoException extends RuntimeException {

    public PublicacaoEventoException(String message, Throwable cause) {
        super(message, cause);
    }

    public PublicacaoEventoException(String message) {
        super(message);
    }
}
