package com.exemplo.pedidos.consumer;

import com.exemplo.pedidos.config.RabbitConfig;
import com.exemplo.pedidos.exception.EventoInvalidoException;
import com.exemplo.pedidos.model.EventoPedido;
import com.exemplo.pedidos.validation.EventoPedidoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor dos eventos de pedido destinados ao processamento de pagamento.
 *
 * <p>Escuta a {@link RabbitConfig#FILA_PAGAMENTO}, valida o conteúdo do evento,
 * executa o processamento simulado de pagamento e registra em log o
 * identificador do pedido processado.</p>
 *
 * <p>Em caso de conteúdo inválido ou erro de conversão, a exceção é propagada
 * (após registro em log do motivo e do id da mensagem) para que o Spring AMQP
 * encaminhe a mensagem à Dead Letter Queue.</p>
 *
 * <p>Requisitos atendidos: 5.1, 5.2, 5.3, 7.1, 7.2, 7.4.</p>
 */
@Component
public class PagamentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoConsumer.class);

    private final EventoPedidoValidator validator;

    public PagamentoConsumer(EventoPedidoValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.FILA_PAGAMENTO)
    public void consumir(EventoPedido evento) {
        try {
            validator.validar(evento);
        } catch (EventoInvalidoException ex) {
            log.error("Encaminhando mensagem a DLQ (fila={}): motivo={}, id={}",
                    RabbitConfig.FILA_PAGAMENTO, ex.getMessage(),
                    evento != null ? evento.id() : null);
            throw ex;
        }

        // Processamento simulado de pagamento.
        log.info("Processando pagamento do pedido: id={}", evento.id());
        log.info("Pedido processado pelo pagamento: id={}", evento.id());
    }
}
