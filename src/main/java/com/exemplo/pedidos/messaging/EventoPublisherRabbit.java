package com.exemplo.pedidos.messaging;

import com.exemplo.pedidos.config.RabbitConfig;
import com.exemplo.pedidos.exception.PublicacaoEventoException;
import com.exemplo.pedidos.model.EventoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link EventoPublisher} baseada em {@link RabbitTemplate}.
 *
 * <p>Publica o {@link EventoPedido} na Exchange_Topic
 * {@link RabbitConfig#EXCHANGE_PEDIDOS} utilizando a routing key
 * {@link RabbitConfig#ROUTING_KEY_PEDIDO_CRIADO}. A serialização para JSON é
 * delegada ao {@code Jackson2JsonMessageConverter} configurado no
 * {@link RabbitTemplate}.</p>
 *
 * <p>Caso a publicação falhe (por exemplo, broker indisponível), o erro é
 * registrado em log e relançado como {@link PublicacaoEventoException}, que a
 * camada web traduz para o status HTTP 502.</p>
 *
 * <p>Requisitos atendidos: 2.1, 2.3, 2.4.</p>
 */
@Component
public class EventoPublisherRabbit implements EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisherRabbit.class);

    private final RabbitTemplate rabbitTemplate;

    public EventoPublisherRabbit(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicar(EventoPedido evento) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_PEDIDOS,
                    RabbitConfig.ROUTING_KEY_PEDIDO_CRIADO,
                    evento);
            log.info("Evento de pedido publicado: id={}, exchange={}, routingKey={}",
                    evento.id(), RabbitConfig.EXCHANGE_PEDIDOS, RabbitConfig.ROUTING_KEY_PEDIDO_CRIADO);
        } catch (AmqpException ex) {
            log.error("Falha ao publicar evento de pedido: id={}, exchange={}, routingKey={}",
                    evento.id(), RabbitConfig.EXCHANGE_PEDIDOS, RabbitConfig.ROUTING_KEY_PEDIDO_CRIADO, ex);
            throw new PublicacaoEventoException(
                    "Falha ao publicar o evento do pedido " + evento.id() + " no RabbitMQ", ex);
        }
    }
}
