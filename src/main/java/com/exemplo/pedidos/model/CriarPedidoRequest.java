package com.exemplo.pedidos.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo da requisicao REST para cadastro de um novo pedido.
 *
 * <p>As anotacoes de Bean Validation sao aplicadas no controller via
 * {@code @Valid}, retornando HTTP 400 quando campos obrigatorios estiverem
 * ausentes ou invalidos.</p>
 *
 * @param cliente    identificacao do cliente; nao pode ser em branco
 * @param itens      itens do pedido; nao pode ser vazio e cada item e validado
 * @param valorTotal valor total do pedido; obrigatorio e positivo
 */
public record CriarPedidoRequest(
        @NotBlank String cliente,
        @NotEmpty @Valid List<ItemPedido> itens,
        @NotNull @Positive BigDecimal valorTotal
) {
}
