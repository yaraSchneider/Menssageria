package com.exemplo.pedidos.service;

import com.exemplo.pedidos.messaging.EventoPublisher;
import com.exemplo.pedidos.model.CriarPedidoRequest;
import com.exemplo.pedidos.model.EventoPedido;
import com.exemplo.pedidos.model.Pedido;
import com.exemplo.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacao padrao de {@link PedidoService}.
 *
 * <p>Orquestra a criacao do pedido: gera um identificador unico (UUID), define
 * o momento de criacao, persiste o pedido via {@link PedidoRepository}, monta o
 * {@link EventoPedido} a partir do pedido salvo e o publica via
 * {@link EventoPublisher}.</p>
 *
 * <p>Requisitos atendidos: 1.1, 1.3, 1.4, 2.1.</p>
 */
@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EventoPublisher eventoPublisher;

    /**
     * Cria o servico com suas dependencias (injecao via construtor).
     *
     * @param pedidoRepository repositorio de pedidos
     * @param eventoPublisher  produtor de eventos de pedido
     */
    public PedidoServiceImpl(PedidoRepository pedidoRepository, EventoPublisher eventoPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.eventoPublisher = eventoPublisher;
    }

    @Override
    public Pedido criarPedido(CriarPedidoRequest request) {
        String id = UUID.randomUUID().toString();
        Instant dataCriacao = Instant.now();

        Pedido pedido = new Pedido(
                id,
                request.cliente(),
                request.itens(),
                request.valorTotal(),
                dataCriacao
        );

        Pedido pedidoSalvo = pedidoRepository.salvar(pedido);

        EventoPedido evento = new EventoPedido(
                pedidoSalvo.id(),
                pedidoSalvo.cliente(),
                pedidoSalvo.itens(),
                pedidoSalvo.valorTotal(),
                pedidoSalvo.dataCriacao()
        );

        eventoPublisher.publicar(evento);

        return pedidoSalvo;
    }

    @Override
    public Optional<Pedido> buscarPorId(String id) {
        return pedidoRepository.buscarPorId(id);
    }
}
