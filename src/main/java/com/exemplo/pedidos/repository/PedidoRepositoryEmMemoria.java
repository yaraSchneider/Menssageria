package com.exemplo.pedidos.repository;

import com.exemplo.pedidos.model.Pedido;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementacao em memoria de {@link PedidoRepository}.
 *
 * <p>Armazena os pedidos em um {@link ConcurrentHashMap} indexado pelo
 * identificador do pedido, garantindo acesso seguro em ambiente concorrente
 * sem necessidade de banco de dados externo.</p>
 */
@Repository
public class PedidoRepositoryEmMemoria implements PedidoRepository {

    private final ConcurrentMap<String, Pedido> armazenamento = new ConcurrentHashMap<>();

    @Override
    public Pedido salvar(Pedido pedido) {
        armazenamento.put(pedido.id(), pedido);
        return pedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(String id) {
        return Optional.ofNullable(armazenamento.get(id));
    }
}
