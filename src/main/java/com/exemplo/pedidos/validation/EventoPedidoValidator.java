package com.exemplo.pedidos.validation;

import com.exemplo.pedidos.exception.EventoInvalidoException;
import com.exemplo.pedidos.model.EventoPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validador de conteúdo do {@link EventoPedido} aplicado pelos consumidores
 * antes do processamento.
 *
 * <p>As regras de conteúdo são: o cliente não pode estar em branco, a lista de
 * itens não pode ser vazia e o valor total deve ser positivo (maior que zero).
 * Um evento é considerado inválido se, e somente se, violar ao menos uma dessas
 * regras.</p>
 *
 * <p>Quando o evento é inválido, {@link #validar(EventoPedido)} lança uma
 * {@link EventoInvalidoException} (não recuperável), levando ao encaminhamento
 * da mensagem à Dead Letter Queue.</p>
 *
 * <p>Requisito atendido: 7.2.</p>
 */
@Component
public class EventoPedidoValidator {

    /**
     * Valida o conteúdo do evento.
     *
     * @param evento evento a ser validado
     * @throws EventoInvalidoException se o evento for nulo ou violar alguma das
     *                                 regras de conteúdo (cliente em branco,
     *                                 itens vazios ou valor total não positivo)
     */
    public void validar(EventoPedido evento) {
        if (evento == null) {
            throw new EventoInvalidoException("Evento de pedido nulo");
        }

        String motivo = motivoInvalidez(evento);
        if (motivo != null) {
            throw new EventoInvalidoException(
                    "Evento de pedido invalido (id=" + evento.id() + "): " + motivo);
        }
    }

    /**
     * Indica se o evento é válido segundo as regras de conteúdo.
     *
     * @param evento evento a ser verificado
     * @return {@code true} se o evento for válido; {@code false} caso contrário
     */
    public boolean isValido(EventoPedido evento) {
        return evento != null && motivoInvalidez(evento) == null;
    }

    /**
     * Retorna o motivo da invalidez do evento ou {@code null} caso ele seja
     * válido.
     */
    private String motivoInvalidez(EventoPedido evento) {
        if (evento.cliente() == null || evento.cliente().isBlank()) {
            return "cliente em branco";
        }
        if (evento.itens() == null || evento.itens().isEmpty()) {
            return "lista de itens vazia";
        }
        if (evento.valorTotal() == null
                || evento.valorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return "valor total nao positivo";
        }
        return null;
    }
}
