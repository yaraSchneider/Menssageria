package com.exemplo.pedidos.service;

import com.exemplo.pedidos.model.CriarPedidoRequest;
import com.exemplo.pedidos.model.Pedido;

import java.util.Optional;

/**
 * Camada de servico responsavel por orquestrar o ciclo de vida de um pedido.
 *
 * <p>Coordena a criacao do pedido (geracao de identificador, definicao do
 * momento de criacao e persistencia) e aciona a publicacao do evento
 * correspondente no RabbitMQ.</p>
 *
 * <p>Requisitos atendidos: 1.1, 1.3, 1.4, 2.1.</p>
 */
public interface PedidoService {

    /**
     * Cria um novo pedido a partir dos dados informados.
     *
     * <p>Gera um identificador unico, define o momento de criacao, persiste o
     * pedido e publica o evento de pedido correspondente.</p>
     *
     * @param request dados do pedido a ser criado
     * @return o pedido criado e persistido
     */
    Pedido criarPedido(CriarPedidoRequest request);

    /**
     * Busca um pedido pelo seu identificador.
     *
     * @param id identificador do pedido
     * @return um {@link Optional} contendo o pedido, ou vazio caso nao exista
     */
    Optional<Pedido> buscarPorId(String id);
}
