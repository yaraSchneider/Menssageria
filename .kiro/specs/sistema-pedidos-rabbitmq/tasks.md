# Plano de Implementação: Sistema de Pedidos com RabbitMQ

## Visão Geral

Este plano converte o design em uma sequência incremental de tarefas de código. Cada tarefa constrói sobre as anteriores e termina integrando os componentes. A ordem é: configuração do projeto e infraestrutura local, modelos de dados, repositório em memória, configuração do RabbitMQ, produtor, serviço, camada web (controller + validação + erros), consumidores e, por fim, os testes (unitários, de propriedade com jqwik, de integração com Testcontainers e de smoke de contexto).

## Tarefas

- [x] 1. Configurar projeto Spring Boot e infraestrutura local
  - Criar projeto Maven Spring Boot com as dependências: `spring-boot-starter-web`, `spring-boot-starter-amqp`, `spring-boot-starter-validation`
  - Adicionar dependências de teste: `spring-boot-starter-test`, `spring-rabbit-test`, `net.jqwik:jqwik`, `org.testcontainers:rabbitmq`, `org.testcontainers:junit-jupiter`
  - Criar a classe principal `Application` com `@SpringBootApplication`
  - Criar `docker-compose.yml` com o serviço `rabbitmq` usando a imagem `rabbitmq:3-management` expondo as portas `5672` e `15672`
  - Criar `application.yml` com a conexão do RabbitMQ e a política de retry do listener (`acknowledge-mode: auto`, `retry.enabled: true`, `max-attempts: 3`, `default-requeue-rejected: false`)
  - _Requisitos: 8.1, 7.5_

- [ ] 2. Implementar modelos de dados
  - [x] 2.1 Criar `ItemPedido`, `CriarPedidoRequest`, `Pedido` e `EventoPedido`
    - `ItemPedido`: campos `produto` (`@NotBlank`) e `quantidade` (`@Positive`)
    - `CriarPedidoRequest`: `cliente` (`@NotBlank`), `itens` (`@NotEmpty`, `@Valid`), `valorTotal` (`@NotNull`, `@Positive`)
    - `Pedido`: `id`, `cliente`, `itens`, `valorTotal`, `dataCriacao` (`Instant`)
    - `EventoPedido`: `id`, `cliente`, `itens`, `valorTotal`, `dataCriacao`, como POJO serializável por Jackson com `equals`/`hashCode`
    - _Requisitos: 1.2, 1.3, 9.1, 9.2_

  - [ ]* 2.2 Escrever teste de propriedade de round-trip JSON do `EventoPedido`
    - **Property 1: Round-trip de serialização JSON do Evento de Pedido**
    - **Validates: Requirements 2.2, 9.1, 9.2, 9.3**
    - Usar jqwik para gerar `EventoPedido` válidos, serializar/desserializar com `ObjectMapper` (com `JavaTimeModule`) e assertar equivalência com o original (mínimo 100 iterações)

- [ ] 3. Implementar repositório em memória
  - [x] 3.1 Criar interface `PedidoRepository` e implementação `PedidoRepositoryEmMemoria`
    - Métodos `salvar(Pedido)` e `buscarPorId(String)` retornando `Optional<Pedido>`
    - Implementação baseada em `ConcurrentHashMap<String, Pedido>`
    - _Requisitos: 1.4_

- [ ] 4. Configurar topologia do RabbitMQ
  - [x] 4.1 Criar `RabbitConfig` com exchange, filas, DLX/DLQ, bindings e conversor JSON
    - Declarar `TopicExchange` `pedidos.exchange`
    - Declarar as filas `fila.estoque`, `fila.pagamento` e `fila.notificacao` com argumentos `x-dead-letter-exchange = pedidos.dlx` e `x-dead-letter-routing-key = pedidos.dlq`
    - Declarar a DLX `pedidos.dlx` (direct) e a fila `pedidos.dlq`, com binding entre elas
    - Criar bindings das três filas de processamento à `pedidos.exchange` usando a routing key `pedido.criado`
    - Registrar bean `Jackson2JsonMessageConverter` e configurar o `RabbitTemplate` para usá-lo
    - _Requisitos: 3.1, 3.2, 3.3, 7.3_

- [ ] 5. Implementar produtor de eventos
  - [x] 5.1 Criar interface `EventoPublisher` e implementação `EventoPublisherRabbit`
    - Publicar o `EventoPedido` na exchange `pedidos.exchange` com a routing key `pedido.criado` via `RabbitTemplate.convertAndSend`
    - Criar `PublicacaoEventoException` e lançá-la quando a publicação falhar, registrando o erro em log
    - _Requisitos: 2.1, 2.3, 2.4_

  - [ ]* 5.2 Escrever teste unitário do `EventoPublisher`
    - Verificar com `RabbitTemplate` mockado que `convertAndSend` é chamado com exchange `pedidos.exchange` e routing key `pedido.criado`
    - Verificar que falha de publicação lança `PublicacaoEventoException`
    - _Requisitos: 2.1, 2.3, 2.4_

- [ ] 6. Implementar a camada de serviço
  - [x] 6.1 Criar interface `PedidoService` e implementação `PedidoServiceImpl`
    - `criarPedido`: gerar `id` (UUID), definir `dataCriacao`, persistir via `PedidoRepository`, montar o `EventoPedido` e publicar via `EventoPublisher`
    - `buscarPorId`: delegar ao repositório
    - _Requisitos: 1.1, 1.3, 1.4, 2.1_

  - [ ]* 6.2 Escrever teste de propriedade de unicidade de identificadores
    - **Property 2: Unicidade dos identificadores de pedido**
    - **Validates: Requirements 1.3**
    - Gerar listas de pedidos válidos, criar todos via serviço (repositório em memória, publisher mockado) e assertar ids não-vazios e mutuamente distintos (mínimo 100 iterações)

  - [ ]* 6.3 Escrever teste de propriedade de round-trip criar/consultar
    - **Property 3: Round-trip de criação e consulta de pedido**
    - **Validates: Requirements 1.4**
    - Gerar pedido válido, criar e buscar pelo id, assertar equivalência dos dados (mínimo 100 iterações)

- [ ] 7. Checkpoint - Garantir que os testes passem
  - Garanta que todos os testes passem; pergunte ao usuário se surgirem dúvidas.

- [ ] 8. Implementar a camada web
  - [x] 8.1 Criar `PedidoController` com os endpoints REST
    - `POST /pedidos` com `@Valid` → `201 Created` com `{ "id": "..." }`
    - `GET /pedidos/{id}` → `200 OK` com o `Pedido` ou lançar `PedidoNaoEncontradoException`
    - _Requisitos: 1.1, 1.2, 1.4, 1.5_

  - [x] 8.2 Criar `PedidoNaoEncontradoException` e `GlobalExceptionHandler`
    - `@RestControllerAdvice` mapeando `MethodArgumentNotValidException` → `400` com lista de campos ausentes/inválidos
    - `PedidoNaoEncontradoException` → `404`
    - `PublicacaoEventoException` → `502` com log do erro
    - _Requisitos: 1.2, 1.5, 2.4_

  - [ ]* 8.3 Escrever testes unitários do `PedidoController` com `MockMvc`
    - Corpo válido → `201` com id; campos ausentes → `400`; consulta inexistente → `404`; falha de publicação → `502` (serviço/publisher mockados)
    - _Requisitos: 1.1, 1.2, 1.4, 1.5, 2.4_

- [ ] 9. Implementar validação de conteúdo e consumidores
  - [x] 9.1 Criar validador de conteúdo do `EventoPedido`
    - Rejeitar evento com cliente em branco, lista de itens vazia ou valor total não positivo, lançando uma exceção não recuperável que leva ao encaminhamento à DLQ
    - _Requisitos: 7.2_

  - [ ]* 9.2 Escrever teste de propriedade de validação de conteúdo do evento
    - **Property 4: Validação de conteúdo do evento**
    - **Validates: Requirements 7.2**
    - Gerar eventos válidos e inválidos (cliente em branco, itens vazios, valor não positivo) e assertar que a validação aceita os válidos e rejeita os inválidos, se e somente se violarem as regras (mínimo 100 iterações)

  - [x] 9.3 Implementar `EstoqueConsumer`, `PagamentoConsumer` e `NotificacaoConsumer`
    - Cada consumidor usa `@RabbitListener` na sua fila, valida o conteúdo, executa o processamento simulado e registra em log o id do pedido processado
    - Em conteúdo inválido/erro de conversão, deixar a exceção propagar para encaminhamento à DLQ, registrando em log o motivo e o id da mensagem
    - _Requisitos: 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 7.1, 7.2, 7.4_

- [ ] 10. Testes de integração e smoke
  - [ ]* 10.1 Escrever teste de smoke do contexto Spring
    - Carregar o `ApplicationContext` e verificar os beans `TopicExchange` (`pedidos.exchange`), as três filas, a DLX/DLQ e os bindings, incluindo os argumentos `x-dead-letter-*` nas filas de processamento
    - _Requisitos: 3.1, 3.2, 3.3, 7.3_

  - [ ]* 10.2 Escrever testes de integração com Testcontainers (`rabbitmq:3-management`)
    - Publicar um `EventoPedido` e verificar a entrega às três filas e a invocação dos consumidores com ack após sucesso
    - Verificar que payload inválido/não desserializável é encaminhado à DLQ
    - Verificar que erro transitório provoca novas tentativas até o limite configurado e, então, encaminhamento à DLQ
    - _Requisitos: 2.1, 3.4, 4.1, 4.2, 5.1, 5.2, 6.1, 6.2, 7.1, 7.2, 7.5_

- [ ] 11. Integração final e documentação de execução
  - [x] 11.1 Conectar os componentes e criar o README
    - Garantir que `Application`, `RabbitConfig`, controller, serviço, publisher e consumidores estejam integrados e inicializando juntos
    - Criar `README.md` com instruções de execução (`docker-compose up`, como rodar a aplicação) e verificação manual do RabbitMQ Management em `http://localhost:15672` (exchange, filas, DLQ, contagem de mensagens e taxa de consumo)
    - _Requisitos: 8.1, 8.2, 8.3_

- [ ] 12. Checkpoint final - Garantir que os testes passem
  - Garanta que todos os testes passem; pergunte ao usuário se surgirem dúvidas.

## Notas

- Tarefas marcadas com `*` são opcionais (testes) e podem ser puladas para um MVP mais rápido.
- Cada tarefa referencia requisitos específicos para rastreabilidade.
- Os checkpoints garantem validação incremental.
- Testes de propriedade (jqwik) validam as 4 propriedades universais de corretude do design.
- Testes unitários, de integração (Testcontainers) e de smoke validam exemplos, edge cases e a topologia do RabbitMQ.
