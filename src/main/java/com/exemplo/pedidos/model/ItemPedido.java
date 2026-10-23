package com.exemplo.pedidos.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Representa um item de um pedido.
 *
 * <p>Implementado como {@code record} Java 17, o que fornece automaticamente
 * {@code equals}/{@code hashCode}/{@code toString} e construtor canonico,
 * alem de ser serializavel/desserializavel pelo Jackson.</p>
 *
 * @param produto    nome do produto; nao pode ser em branco
 * @param quantidade quantidade do produto; deve ser positiva
 */
public record ItemPedido(
        @NotBlank String produto,
        @Positive int quantidade
) {
}
