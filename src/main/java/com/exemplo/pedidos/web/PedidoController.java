package com.exemplo.pedidos.web;

import com.exemplo.pedidos.exception.PedidoNaoEncontradoException;
import com.exemplo.pedidos.model.CriarPedidoRequest;
import com.exemplo.pedidos.model.Pedido;
import com.exemplo.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador REST da Pedido_API.
 *
 * <p>Expõe os endpoints de cadastro ({@code POST /pedidos}) e consulta
 * ({@code GET /pedidos/{id}}) de pedidos. A validação do corpo da requisição é
 * feita via {@code @Valid}; erros de validação, de consulta e de publicação são
 * traduzidos para os códigos HTTP apropriados pelo
 * {@code GlobalExceptionHandler}.</p>
 *
 * <p>Requisitos atendidos: 1.1, 1.2, 1.4, 1.5.</p>
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    /**
     * Cria o controlador com sua dependência de serviço (injeção via construtor).
     *
     * @param pedidoService serviço de orquestração de pedidos
     */
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * Cadastra um novo pedido.
     *
     * <p>Valida o corpo da requisição e, em caso de sucesso, retorna HTTP 201
     * com o identificador do pedido criado e o cabeçalho {@code Location}
     * apontando para o recurso recém-criado.</p>
     *
     * @param request dados do pedido a ser criado
     * @return resposta HTTP 201 com o identificador do pedido
     */
    @PostMapping
    public ResponseEntity<CriarPedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest request) {
        Pedido pedido = pedidoService.criarPedido(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pedido.id())
                .toUri();

        return ResponseEntity.created(location).body(new CriarPedidoResponse(pedido.id()));
    }

    /**
     * Consulta um pedido pelo seu identificador.
     *
     * @param id identificador do pedido
     * @return HTTP 200 com os dados do pedido
     * @throws PedidoNaoEncontradoException se o pedido não existir (traduzido
     *                                      para HTTP 404 pelo handler global)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(@PathVariable String id) {
        Pedido pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));

        return ResponseEntity.ok(pedido);
    }

    /**
     * Corpo de resposta do cadastro de pedido, contendo apenas o identificador
     * gerado.
     *
     * @param id identificador do pedido criado
     */
    public record CriarPedidoResponse(String id) {
    }
}
