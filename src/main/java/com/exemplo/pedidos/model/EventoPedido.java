package com.exemplo.pedidos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mensagem (evento) publicada no RabbitMQ quando um pedido e criado.
 *
 * <p>E este objeto que trafega pelo RabbitMQ serializado em JSON. Implementado
 * como {@code record} Java 17, fornece {@code equals}/{@code hashCode}
 * automaticamente (necessarios para a propriedade de round-trip do Requisito 9)
 * e e serializavel/desserializavel pelo Jackson.</p>
 *
 * <p>O construtor e anotado com {@link JsonCreator}/{@link JsonProperty} para
 * garantir a desserializacao mesmo sem informacao de nomes de parametros no
 * bytecode.</p>
 *
 * @param id          identificador do pedido
 * @param cliente     identificacao do cliente
 * @param itens       itens do pedido
 * @param valorTotal  valor total do pedido
 * @param dataCriacao momento da criacao do pedido
 */
public record EventoPedido(
        @JsonProperty("id") String id,
        @JsonProperty("cliente") String cliente,
        @JsonProperty("itens") List<ItemPedido> itens,
        @JsonProperty("valorTotal") BigDecimal valorTotal,
        @JsonProperty("dataCriacao") Instant dataCriacao
) {

    @JsonCreator
    public EventoPedido {
    }
}
