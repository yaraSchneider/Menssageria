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
 * Consumidor dos eventos de pedido destinados ao envio de notificação.
 *
 * <p>Escuta a {@link RabbitConfig#FILA_NOTIFICACAO}, valida o conteúdo do
 * evento, simula o envio da notificação (apenas via log, sem envio real de
 * e-mail) e registra em log o identificador do pedido notificado.</p>
 *
 * <p>Em caso de conteúdo inválido ou erro de conversão, a exceção é propagada
 * (após registro em log do motivo e do id da mensagem) para que o Spring AMQP
 * encaminhe a mensagem à Dead Letter Queue.</p>
 *
 * <p>Requisitos atendidos: 6.1, 6.2, 6.3, 7.1, 7.2, 7.4.</p>
 */
@Component
public class NotificacaoConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoConsumer.class);

    private final EventoPedidoValidator validator;

    public NotificacaoConsumer(EventoPedidoValidator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACAO)
    public void consumir(EventoPedido evento) {
        try {
            validator.validar(evento);
        } catch (EventoInvalidoException ex) {
            log.error("Encaminhando mensagem a DLQ (fila={}): motivo={}, id={}",
                    RabbitConfig.FILA_NOTIFICACAO, ex.getMessage(),
                    evento != null ? evento.id() : null);
            throw ex;
        }

        // Envio de notificacao simulado via log (nenhum e-mail real e enviado).
        log.info("Enviando notificacao do pedido: id={}", evento.id());
        log.info("Pedido notificado: id={}", evento.id());
    }
}
