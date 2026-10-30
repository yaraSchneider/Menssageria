package com.exemplo.pedidos.service;

import com.exemplo.pedidos.exception.PublicacaoEventoException;
import com.exemplo.pedidos.messaging.EventoPublisher;
import com.exemplo.pedidos.model.CriarPedidoRequest;
import com.exemplo.pedidos.model.EventoPedido;
import com.exemplo.pedidos.model.ItemPedido;
import com.exemplo.pedidos.model.Pedido;
import com.exemplo.pedidos.repository.PedidoRepositoryEmMemoria;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PedidoServiceImplTest {

    private final PedidoRepositoryEmMemoria repository = spy(new PedidoRepositoryEmMemoria());
    private final EventoPublisher publisher = mock(EventoPublisher.class);
    private final PedidoServiceImpl service = new PedidoServiceImpl(repository, publisher);

    @Test
    void criaPedidoPublicaEventoESalvaPedido() {
        CriarPedidoRequest request = novoPedido();

        Pedido pedido = service.criarPedido(request);

        assertThat(pedido.id()).isNotBlank();
        assertThat(repository.buscarPorId(pedido.id())).contains(pedido);
        verify(publisher).publicar(new EventoPedido(
                pedido.id(),
                pedido.cliente(),
                pedido.itens(),
                pedido.valorTotal(),
                pedido.dataCriacao()
        ));
    }

    @Test
    void removePedidoSalvoQuandoPublicacaoFalha() {
        doThrow(new PublicacaoEventoException("Broker indisponivel"))
                .when(publisher).publicar(any(EventoPedido.class));

        assertThrows(PublicacaoEventoException.class, () -> service.criarPedido(novoPedido()));

        ArgumentCaptor<Pedido> savedPedido = ArgumentCaptor.forClass(Pedido.class);
        verify(repository).salvar(savedPedido.capture());
        verify(repository).removerPorId(savedPedido.getValue().id());
        assertThat(repository.buscarPorId(savedPedido.getValue().id())).isEmpty();
    }

    private static CriarPedidoRequest novoPedido() {
        return new CriarPedidoRequest(
                "Maria Silva",
                List.of(new ItemPedido("Caneca", 2)),
                new BigDecimal("39.90")
        );
    }
}
