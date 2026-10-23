package com.exemplo.pedidos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Entidade que representa um pedido persistido em memoria.
 *
 * <p>Implementado como {@code record} Java 17, fornecendo
 * {@code equals}/{@code hashCode}/{@code toString} automaticamente.</p>
 *
 * @param id          identificador unico gerado na criacao
 * @param cliente     identificacao do cliente
 * @param itens       itens do pedido
 * @param valorTotal  valor total do pedido
 * @param dataCriacao momento da criacao do pedido
 */
public record Pedido(
        String id,
        String cliente,
        List<ItemPedido> itens,
        BigDecimal valorTotal,
        Instant dataCriacao
) {
}
