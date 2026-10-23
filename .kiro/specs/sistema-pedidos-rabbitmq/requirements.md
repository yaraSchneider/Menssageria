# Documento de Requisitos

## Introdução

Este documento descreve os requisitos de um sistema de pedidos baseado em processamento assíncrono utilizando Spring Boot, API REST e RabbitMQ. O sistema permite que clientes cadastrem pedidos por meio de uma API REST. Ao cadastrar um pedido, a API publica um evento em uma exchange do tipo TOPIC no RabbitMQ. Esse evento é roteado, por meio de routing keys, para filas específicas consumidas de forma independente por três consumidores: estoque, pagamento e notificação.

A arquitetura segue o fluxo:

```
Cliente -> POST /pedidos -> Pedido API -> RabbitMQ (Exchange TOPIC) -> Estoque Consumer
                                                                    -> Pagamento Consumer
                                                                    -> Notificacao Consumer
```

O sistema deve tratar mensagens inválidas, oferecer monitoramento via RabbitMQ Management e garantir desacoplamento entre produção e consumo dos eventos de pedido.

## Glossário

- **Pedido_API**: Componente Spring Boot que expõe endpoints REST para cadastro e consulta de pedidos e atua como produtor de eventos no RabbitMQ.
- **Pedido**: Entidade que representa uma solicitação de compra contendo identificador, itens, quantidade, valor total e dados do cliente.
- **Evento_Pedido**: Mensagem serializada (JSON) que representa um pedido cadastrado e é publicada no RabbitMQ.
- **RabbitMQ**: Broker de mensageria AMQP responsável por roteamento, enfileiramento e entrega das mensagens.
- **Exchange_Topic**: Exchange do tipo TOPIC no RabbitMQ responsável por rotear eventos de pedido com base em routing keys.
- **Routing_Key**: Chave de roteamento associada a um evento que define para quais filas a mensagem é entregue.
- **Fila_Estoque**: Fila que recebe eventos de pedido destinados ao processamento de estoque.
- **Fila_Pagamento**: Fila que recebe eventos de pedido destinados ao processamento de pagamento.
- **Fila_Notificacao**: Fila que recebe eventos de pedido destinados ao envio de notificações.
- **Estoque_Consumer**: Componente consumidor que processa eventos da Fila_Estoque.
- **Pagamento_Consumer**: Componente consumidor que processa eventos da Fila_Pagamento.
- **Notificacao_Consumer**: Componente consumidor que processa eventos da Fila_Notificacao.
- **Dead_Letter_Queue**: Fila de mensagens mortas (DLQ) que recebe mensagens que não puderam ser processadas ou são inválidas.
- **RabbitMQ_Management**: Interface de gerenciamento e monitoramento do RabbitMQ.
- **Mensagem_Invalida**: Mensagem que não pode ser desserializada ou que não atende às regras de validação do evento de pedido.

## Requisitos

### Requisito 1: Cadastro de Pedido via API REST

**História de Usuário:** Como cliente, quero cadastrar um pedido através de uma API REST, para que minha solicitação de compra seja registrada e processada.

#### Critérios de Aceitação

1. WHEN uma requisição HTTP POST é recebida no endpoint /pedidos com um corpo válido, THE Pedido_API SHALL criar um Pedido e retornar o status HTTP 201 com o identificador do Pedido.
2. IF uma requisição HTTP POST é recebida no endpoint /pedidos com um corpo que não contém os campos obrigatórios, THEN THE Pedido_API SHALL retornar o status HTTP 400 com uma mensagem descrevendo os campos ausentes.
3. WHEN um Pedido é criado, THE Pedido_API SHALL atribuir um identificador único ao Pedido.
4. WHEN uma requisição HTTP GET é recebida no endpoint /pedidos/{id} para um Pedido existente, THE Pedido_API SHALL retornar o status HTTP 200 com os dados do Pedido.
5. IF uma requisição HTTP GET é recebida no endpoint /pedidos/{id} para um identificador inexistente, THEN THE Pedido_API SHALL retornar o status HTTP 404.

### Requisito 2: Publicação de Evento de Pedido

**História de Usuário:** Como integrador do sistema, quero que cada pedido cadastrado gere um evento publicado no RabbitMQ, para que o processamento ocorra de forma assíncrona e desacoplada.

#### Critérios de Aceitação

1. WHEN um Pedido é criado com sucesso, THE Pedido_API SHALL publicar um Evento_Pedido na Exchange_Topic.
2. WHEN um Evento_Pedido é publicado, THE Pedido_API SHALL serializar o Evento_Pedido no formato JSON.
3. WHEN um Evento_Pedido é publicado, THE Pedido_API SHALL associar ao Evento_Pedido uma Routing_Key correspondente ao tipo de evento de pedido.
4. IF a publicação de um Evento_Pedido na Exchange_Topic falha, THEN THE Pedido_API SHALL registrar o erro em log e retornar o status HTTP 502.

### Requisito 3: Roteamento por Exchange TOPIC e Routing Keys

**História de Usuário:** Como arquiteto do sistema, quero rotear eventos de pedido por meio de uma exchange TOPIC usando routing keys, para que cada consumidor receba os eventos de seu interesse.

#### Critérios de Aceitação

1. THE RabbitMQ SHALL declarar uma Exchange_Topic do tipo topic para os eventos de pedido.
2. THE RabbitMQ SHALL declarar a Fila_Estoque, a Fila_Pagamento e a Fila_Notificacao.
3. THE RabbitMQ SHALL vincular a Fila_Estoque, a Fila_Pagamento e a Fila_Notificacao à Exchange_Topic utilizando padrões de Routing_Key.
4. WHEN um Evento_Pedido é publicado na Exchange_Topic com uma Routing_Key que corresponde ao padrão de vínculo de uma fila, THE RabbitMQ SHALL entregar o Evento_Pedido a essa fila.

### Requisito 4: Consumo de Eventos de Estoque

**História de Usuário:** Como responsável pelo estoque, quero que os pedidos sejam processados por um consumidor de estoque, para que a disponibilidade dos itens seja verificada de forma assíncrona.

#### Critérios de Aceitação

1. WHEN um Evento_Pedido é entregue à Fila_Estoque, THE Estoque_Consumer SHALL consumir o Evento_Pedido e executar o processamento de estoque.
2. WHEN o Estoque_Consumer conclui o processamento de um Evento_Pedido com sucesso, THE Estoque_Consumer SHALL confirmar (ack) a mensagem ao RabbitMQ.
3. WHEN o Estoque_Consumer processa um Evento_Pedido, THE Estoque_Consumer SHALL registrar em log o identificador do Pedido processado.

### Requisito 5: Consumo de Eventos de Pagamento

**História de Usuário:** Como responsável pelo financeiro, quero que os pedidos sejam processados por um consumidor de pagamento, para que a cobrança seja iniciada de forma assíncrona.

#### Critérios de Aceitação

1. WHEN um Evento_Pedido é entregue à Fila_Pagamento, THE Pagamento_Consumer SHALL consumir o Evento_Pedido e executar o processamento de pagamento.
2. WHEN o Pagamento_Consumer conclui o processamento de um Evento_Pedido com sucesso, THE Pagamento_Consumer SHALL confirmar (ack) a mensagem ao RabbitMQ.
3. WHEN o Pagamento_Consumer processa um Evento_Pedido, THE Pagamento_Consumer SHALL registrar em log o identificador do Pedido processado.

### Requisito 6: Consumo de Eventos de Notificação

**História de Usuário:** Como cliente, quero receber uma notificação sobre o meu pedido, para que eu seja informado do andamento da solicitação.

#### Critérios de Aceitação

1. WHEN um Evento_Pedido é entregue à Fila_Notificacao, THE Notificacao_Consumer SHALL consumir o Evento_Pedido e executar o envio da notificação.
2. WHEN o Notificacao_Consumer conclui o envio de uma notificação com sucesso, THE Notificacao_Consumer SHALL confirmar (ack) a mensagem ao RabbitMQ.
3. WHEN o Notificacao_Consumer processa um Evento_Pedido, THE Notificacao_Consumer SHALL registrar em log o identificador do Pedido notificado.

### Requisito 7: Tratamento de Mensagens Inválidas

**História de Usuário:** Como operador do sistema, quero que mensagens inválidas sejam tratadas e isoladas, para que falhas de processamento não bloqueiem as filas nem percam dados.

#### Critérios de Aceitação

1. IF um consumidor recebe uma Mensagem_Invalida que não pode ser desserializada, THEN THE consumidor SHALL encaminhar a Mensagem_Invalida para a Dead_Letter_Queue.
2. IF um consumidor recebe um Evento_Pedido que não atende às regras de validação de conteúdo, THEN THE consumidor SHALL encaminhar a mensagem para a Dead_Letter_Queue.
3. THE RabbitMQ SHALL declarar uma Dead_Letter_Queue vinculada às filas de processamento.
4. WHEN uma mensagem é encaminhada para a Dead_Letter_Queue, THE consumidor SHALL registrar em log o motivo da rejeição e o identificador da mensagem.
5. IF o processamento de um Evento_Pedido falha por um erro transitório, THEN THE consumidor SHALL reencaminhar a mensagem para nova tentativa até um limite de tentativas configurável antes de encaminhar para a Dead_Letter_Queue.

### Requisito 8: Monitoramento via RabbitMQ Management

**História de Usuário:** Como operador do sistema, quero monitorar filas e mensagens pelo RabbitMQ Management, para que eu possa acompanhar a saúde do sistema de mensageria.

#### Critérios de Aceitação

1. THE RabbitMQ SHALL habilitar o plugin RabbitMQ_Management.
2. WHERE o plugin RabbitMQ_Management está habilitado, THE RabbitMQ_Management SHALL exibir a Exchange_Topic, a Fila_Estoque, a Fila_Pagamento, a Fila_Notificacao e a Dead_Letter_Queue.
3. WHERE o plugin RabbitMQ_Management está habilitado, THE RabbitMQ_Management SHALL exibir a quantidade de mensagens enfileiradas e a taxa de consumo de cada fila.

### Requisito 9: Serialização e Round-Trip do Evento de Pedido

**História de Usuário:** Como integrador do sistema, quero que a serialização e desserialização do evento de pedido sejam consistentes, para que nenhum dado seja perdido ou corrompido durante o trânsito pelo RabbitMQ.

#### Critérios de Aceitação

1. THE Pedido_API SHALL serializar um Evento_Pedido em JSON antes da publicação.
2. THE consumidor SHALL desserializar o JSON recebido em um Evento_Pedido antes do processamento.
3. FOR ALL Evento_Pedido válido, serializar em JSON e depois desserializar SHALL produzir um Evento_Pedido equivalente ao original (propriedade de round-trip).
