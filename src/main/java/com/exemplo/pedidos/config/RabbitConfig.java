package com.exemplo.pedidos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao da topologia do RabbitMQ para o sistema de pedidos.
 *
 * <p>Declara, via beans, a exchange TOPIC de pedidos, as tres filas de
 * processamento (estoque, pagamento e notificacao) com argumentos de Dead
 * Letter, a Dead Letter Exchange (DLX) e a Dead Letter Queue (DLQ), os bindings
 * correspondentes e o conversor JSON utilizado pelo {@link RabbitTemplate}.</p>
 *
 * <p>Os nomes dos recursos sao expostos como constantes publicas para que o
 * produtor e os consumidores possam referencia-los sem duplicar strings.</p>
 *
 * <p>Requisitos atendidos: 3.1, 3.2, 3.3, 7.3.</p>
 */
@Configuration
public class RabbitConfig {

    /** Exchange TOPIC para onde os eventos de pedido sao publicados. */
    public static final String EXCHANGE_PEDIDOS = "pedidos.exchange";

    /** Routing key unica usada na publicacao e nos bindings das tres filas. */
    public static final String ROUTING_KEY_PEDIDO_CRIADO = "pedido.criado";

    /** Fila consumida pelo processamento de estoque. */
    public static final String FILA_ESTOQUE = "fila.estoque";

    /** Fila consumida pelo processamento de pagamento. */
    public static final String FILA_PAGAMENTO = "fila.pagamento";

    /** Fila consumida pelo processamento de notificacao. */
    public static final String FILA_NOTIFICACAO = "fila.notificacao";

    /** Dead Letter Exchange (direct) que roteia mensagens mortas para a DLQ. */
    public static final String DLX_PEDIDOS = "pedidos.dlx";

    /** Dead Letter Queue que recebe as mensagens mortas das filas de processamento. */
    public static final String DLQ_PEDIDOS = "pedidos.dlq";

    // ------------------------------------------------------------------
    // Exchange principal (TOPIC) - Requisito 3.1
    // ------------------------------------------------------------------

    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(EXCHANGE_PEDIDOS, true, false);
    }

    // ------------------------------------------------------------------
    // Filas de processamento (durable) com argumentos de Dead Letter
    // - Requisitos 3.2 e 7.3
    // ------------------------------------------------------------------

    @Bean
    public Queue filaEstoque() {
        return filaDeProcessamento(FILA_ESTOQUE);
    }

    @Bean
    public Queue filaPagamento() {
        return filaDeProcessamento(FILA_PAGAMENTO);
    }

    @Bean
    public Queue filaNotificacao() {
        return filaDeProcessamento(FILA_NOTIFICACAO);
    }

    /**
     * Cria uma fila durable configurada para encaminhar mensagens mortas
     * (rejeitadas/nao recuperaveis) para a DLX de pedidos.
     *
     * @param nome nome da fila
     * @return fila durable com argumentos {@code x-dead-letter-exchange} e
     *         {@code x-dead-letter-routing-key}
     */
    private Queue filaDeProcessamento(String nome) {
        return QueueBuilder.durable(nome)
                .withArgument("x-dead-letter-exchange", DLX_PEDIDOS)
                .withArgument("x-dead-letter-routing-key", DLQ_PEDIDOS)
                .build();
    }

    // ------------------------------------------------------------------
    // Dead Letter Exchange (direct) + Dead Letter Queue - Requisito 7.3
    // ------------------------------------------------------------------

    @Bean
    public DirectExchange pedidosDlx() {
        return new DirectExchange(DLX_PEDIDOS, true, false);
    }

    @Bean
    public Queue pedidosDlq() {
        return QueueBuilder.durable(DLQ_PEDIDOS).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(pedidosDlq())
                .to(pedidosDlx())
                .with(DLQ_PEDIDOS);
    }

    // ------------------------------------------------------------------
    // Bindings das filas de processamento a exchange TOPIC - Requisito 3.3
    // ------------------------------------------------------------------

    @Bean
    public Binding estoqueBinding() {
        return BindingBuilder.bind(filaEstoque())
                .to(pedidosExchange())
                .with(ROUTING_KEY_PEDIDO_CRIADO);
    }

    @Bean
    public Binding pagamentoBinding() {
        return BindingBuilder.bind(filaPagamento())
                .to(pedidosExchange())
                .with(ROUTING_KEY_PEDIDO_CRIADO);
    }

    @Bean
    public Binding notificacaoBinding() {
        return BindingBuilder.bind(filaNotificacao())
                .to(pedidosExchange())
                .with(ROUTING_KEY_PEDIDO_CRIADO);
    }

    // ------------------------------------------------------------------
    // Conversor JSON e RabbitTemplate
    // ------------------------------------------------------------------

    /**
     * Conversor JSON baseado em Jackson. Registra o {@link JavaTimeModule} para
     * que tipos de data/hora como {@code Instant} sejam serializados e
     * desserializados corretamente.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
