package com.exemplo.pedidos.repository;

import com.exemplo.pedidos.model.Pedido;

import java.util.Optional;

/**
 * Repositorio de pedidos.
 *
 * <p>Abstrai a persistencia dos pedidos. Para fins didaticos, a implementacao
 * padrao mantem os dados em memoria (ver {@link PedidoRepositoryEmMemoria}).</p>
 */
public interface PedidoRepository {

    /**
     * Persiste o pedido informado.
     *
     * @param pedido pedido a ser salvo
     * @return o pedido salvo
     */
    Pedido salvar(Pedido pedido);

    /**
     * Busca um pedido pelo seu identificador.
     *
     * @param id identificador do pedido
     * @return um {@link Optional} contendo o pedido, ou vazio caso nao exista
     */
    Optional<Pedido> buscarPorId(String id);
}
