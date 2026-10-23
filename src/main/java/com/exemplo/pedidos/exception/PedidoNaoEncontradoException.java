package com.exemplo.pedidos.exception;

/**
 * Exceção lançada quando um pedido consultado por identificador não existe.
 *
 * <p>É uma {@link RuntimeException} para manter a camada web enxuta. O
 * {@code GlobalExceptionHandler} a traduz para o status HTTP 404, indicando
 * que o pedido solicitado não foi encontrado.</p>
 *
 * <p>Requisito atendido: 1.5.</p>
 */
public class PedidoNaoEncontradoException extends RuntimeException {

    /**
     * Cria a exceção para o identificador de pedido informado.
     *
     * @param id identificador do pedido não encontrado
     */
    public PedidoNaoEncontradoException(String id) {
        super("Pedido nao encontrado: " + id);
    }
}
