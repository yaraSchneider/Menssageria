# Sistema de Pedidos com RabbitMQ

Projeto didático de processamento assíncrono de pedidos usando **Spring Boot**, **API REST** e **RabbitMQ**. Ao cadastrar um pedido pela API REST, um evento é publicado em uma exchange do tipo **TOPIC** e roteado para três filas independentes, consumidas por três consumidores: **estoque**, **pagamento** e **notificação**.

## Visão Geral

Uma única aplicação Spring Boot:

- Expõe endpoints REST para cadastro (`POST /pedidos`) e consulta (`GET /pedidos/{id}`).
- Persiste pedidos **em memória** (sem banco de dados externo).
- Publica um `EventoPedido` em JSON na exchange TOPIC `pedidos.exchange`.
- Possui três consumidores independentes que validam e processam o evento (processamento simulado via log).
- Trata mensagens inválidas e falhas transitórias com **retry** e **Dead Letter Queue (DLQ)**.

## Arquitetura e Fluxo

```
Cliente -> POST /pedidos -> PedidoController -> PedidoService -> PedidoRepository (memória)
                                                      |
                                                      v
                                              EventoPublisher (JSON)
                                                      |
                                                      v
                                   RabbitMQ (Exchange TOPIC "pedidos.exchange")
                                       |              |                 |
                              fila.estoque    fila.pagamento    fila.notificacao
                                       |              |                 |
                           EstoqueConsumer  PagamentoConsumer  NotificacaoConsumer
                                       \______________|_________________/
                                                      |  (mensagem inválida / falha persistente)
                                                      v
                                       pedidos.dlx (DLX) -> pedidos.dlq (DLQ)
```

### Topologia do RabbitMQ

| Recurso        | Nome               | Tipo   | Observação                                   |
|----------------|--------------------|--------|----------------------------------------------|
| Exchange       | `pedidos.exchange` | topic  | Roteamento por routing key                   |
| Fila           | `fila.estoque`     | durable| Binding `pedido.criado`                      |
| Fila           | `fila.pagamento`   | durable| Binding `pedido.criado`                      |
| Fila           | `fila.notificacao` | durable| Binding `pedido.criado`                      |
| Exchange (DLX) | `pedidos.dlx`      | direct | Dead Letter Exchange                         |
| Fila (DLQ)     | `pedidos.dlq`      | durable| Recebe mensagens mortas das três filas       |

A publicação usa a routing key única **`pedido.criado`**, e as três filas estão vinculadas a essa chave, de modo que todo pedido criado é roteado simultaneamente para estoque, pagamento e notificação. Cada fila de processamento é declarada com os argumentos `x-dead-letter-exchange = pedidos.dlx` e `x-dead-letter-routing-key = pedidos.dlq`.

## Pré-requisitos

- **JDK 17**
- **Docker** e **Docker Compose** (para subir o RabbitMQ)
- Não é necessário instalar Maven: o projeto inclui o wrapper (`./mvnw` / `mvnw.cmd`)

## Como Executar

### 1. Subir o RabbitMQ

O `docker-compose.yml` usa a imagem `rabbitmq:3-management`, que já habilita o plugin de Management.

```bash
docker-compose up -d
```

Portas expostas:

- `5672` — porta AMQP (usada pela aplicação)
- `15672` — interface do RabbitMQ Management

### 2. Rodar a aplicação

Em outro terminal, na raiz do projeto:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Ao iniciar, o Spring AMQP declara automaticamente a exchange, as filas, a DLX e a DLQ, e os três consumidores passam a escutar suas filas.

### 3. Parar os serviços

```bash
docker-compose down
```

## Endpoints da API

### `POST /pedidos` — cadastrar um pedido

Exemplo de requisição com corpo válido:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
        "cliente": "Maria Silva",
        "itens": [
          { "produto": "Caneca", "quantidade": 2 },
          { "produto": "Caderno", "quantidade": 1 }
        ],
        "valorTotal": 79.90
      }'
```

Resposta esperada — **201 Created** (com cabeçalho `Location` apontando para o recurso criado):

```json
{ "id": "3f2504e0-4f89-41d3-9a0c-0305e82c3301" }
```

Exemplo de corpo inválido (campos ausentes/invalidos) — **400 Bad Request**:

```bash
curl -i -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{ "cliente": "", "itens": [], "valorTotal": -10 }'
```

Resposta esperada:

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "mensagem": "Requisicao invalida: campos obrigatorios ausentes ou invalidos",
  "campos": [
    { "campo": "cliente", "mensagem": "must not be blank" },
    { "campo": "itens", "mensagem": "must not be empty" },
    { "campo": "valorTotal", "mensagem": "must be greater than 0" }
  ]
}
```

### `GET /pedidos/{id}` — consultar um pedido

```bash
curl -i http://localhost:8080/pedidos/3f2504e0-4f89-41d3-9a0c-0305e82c3301
```

Resposta esperada — **200 OK**:

```json
{
  "id": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "cliente": "Maria Silva",
  "itens": [
    { "produto": "Caneca", "quantidade": 2 },
    { "produto": "Caderno", "quantidade": 1 }
  ],
  "valorTotal": 79.90,
  "dataCriacao": "2024-01-01T12:00:00Z"
}
```

Para um identificador inexistente — **404 Not Found**.

### Códigos de resposta

| Código | Situação                                                        |
|--------|-----------------------------------------------------------------|
| `201`  | Pedido criado com sucesso (retorna o `id`)                      |
| `400`  | Corpo inválido: campos obrigatórios ausentes ou inválidos       |
| `404`  | Consulta de pedido inexistente                                  |
| `502`  | Falha ao publicar o evento no RabbitMQ (ex.: broker indisponível)|

> Dica: para observar o `502`, cadastre um pedido com o RabbitMQ parado (`docker-compose down`). A API registra o erro em log e responde `502 Bad Gateway`.

## Monitoramento via RabbitMQ Management

Com o RabbitMQ em execução, acesse a interface de gerenciamento:

- **URL:** http://localhost:15672
- **Usuário:** `guest`
- **Senha:** `guest`

O que verificar (Requisitos 8.1, 8.2 e 8.3):

1. **Exchange** — aba **Exchanges**: confirme a presença de `pedidos.exchange` (tipo `topic`) e de `pedidos.dlx` (tipo `direct`).
2. **Filas** — aba **Queues and Streams**: confirme as três filas de processamento (`fila.estoque`, `fila.pagamento`, `fila.notificacao`) e a DLQ (`pedidos.dlq`).
3. **Contagem de mensagens** — para cada fila, acompanhe as colunas **Ready** e **Unacked** (quantidade de mensagens enfileiradas).
4. **Taxa de consumo** — ao clicar em uma fila, o painel **Message rates** mostra as taxas de **publish**, **deliver** e **ack** (consumo).

### Observando o fluxo normal

Ao enviar um `POST /pedidos` válido, o evento é roteado para as três filas. Como os consumidores estão ativos, as mensagens são consumidas quase imediatamente (ack), e você verá a taxa de consumo subir no painel de **Message rates** de cada fila.

### Observando o fluxo para a DLQ

Mensagens inválidas ou que falham de forma persistente seguem para a DLQ. Isso acontece quando:

- O conteúdo do evento viola as regras de validação (cliente em branco, lista de itens vazia ou valor total não positivo); ou
- A mensagem não pode ser desserializada; ou
- Ocorre um erro transitório que esgota o limite de tentativas (`max-attempts: 3`).

Nesses casos, o Spring AMQP (com `default-requeue-rejected: false` e os argumentos `x-dead-letter-*` das filas) encaminha a mensagem para `pedidos.dlx` e, dela, para `pedidos.dlq`. No Management, a fila `pedidos.dlq` passará a acumular mensagens (coluna **Ready**), enquanto os consumidores registram em log o motivo da rejeição e o id da mensagem.

> Para forçar uma mensagem à DLQ de forma didática, publique manualmente pela aba **Exchanges** do Management uma mensagem na `pedidos.exchange` com routing key `pedido.criado` e um payload inválido (por exemplo, um JSON com `valorTotal` zero ou sem `itens`). Acompanhe o aumento da contagem em `pedidos.dlq`.

## Estrutura do Projeto

```
src/main/java/com/exemplo/pedidos
├── Application.java                 # @SpringBootApplication (base de component scanning)
├── config/RabbitConfig.java         # exchange, filas, DLX/DLQ, bindings, conversor JSON, RabbitTemplate
├── web/                             # PedidoController + GlobalExceptionHandler
├── service/                         # PedidoService + PedidoServiceImpl
├── messaging/                       # EventoPublisher + EventoPublisherRabbit (produtor)
├── consumer/                        # EstoqueConsumer, PagamentoConsumer, NotificacaoConsumer
├── validation/EventoPedidoValidator # validação de conteúdo do evento
├── repository/                      # PedidoRepository + PedidoRepositoryEmMemoria
├── model/                           # CriarPedidoRequest, ItemPedido, Pedido, EventoPedido
└── exception/                       # exceções de domínio (404, 502, evento inválido)
```

Todos os componentes estão sob o pacote base `com.exemplo.pedidos`, de modo que são descobertos automaticamente pelo component scanning do Spring Boot e inicializam juntos com a aplicação.

## Configuração de Retry e DLQ

Definida em `src/main/resources/application.yml`:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: auto        # sucesso = ack; exceção = nack/retry
        default-requeue-rejected: false
        retry:
          enabled: true
          max-attempts: 3             # limite configurável de tentativas
          initial-interval: 1000ms
```
