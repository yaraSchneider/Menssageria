package com.exemplo.pedidos.web;

import com.exemplo.pedidos.exception.PedidoNaoEncontradoException;
import com.exemplo.pedidos.exception.PublicacaoEventoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Tratamento global de erros da camada web.
 *
 * <p>Centraliza a tradução de exceções em respostas HTTP com um formato de erro
 * consistente ({@link ErroResponse}):</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → HTTP 400, com a lista de
 *       campos ausentes/inválidos detectados pela validação do corpo;</li>
 *   <li>{@link PedidoNaoEncontradoException} → HTTP 404;</li>
 *   <li>{@link PublicacaoEventoException} → HTTP 502, registrando o erro em log.</li>
 * </ul>
 *
 * <p>Requisitos atendidos: 1.2, 1.5, 2.4.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata falhas de validação do corpo da requisição, retornando HTTP 400 com
     * a lista de campos inválidos e suas mensagens.
     *
     * @param ex exceção de validação lançada pelo {@code @Valid}
     * @return resposta HTTP 400 com o detalhamento dos campos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::paraErroCampo)
                .toList();

        ErroResponse corpo = new ErroResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida: campos obrigatorios ausentes ou invalidos",
                campos
        );

        return ResponseEntity.badRequest().body(corpo);
    }

    /**
     * Trata a consulta de um pedido inexistente, retornando HTTP 404.
     *
     * @param ex exceção indicando que o pedido não foi encontrado
     * @return resposta HTTP 404
     */
    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(PedidoNaoEncontradoException ex) {
        ErroResponse corpo = new ErroResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpo);
    }

    /**
     * Trata falhas de publicação do evento no RabbitMQ, registrando o erro em
     * log e retornando HTTP 502.
     *
     * @param ex exceção de falha na publicação
     * @return resposta HTTP 502
     */
    @ExceptionHandler(PublicacaoEventoException.class)
    public ResponseEntity<ErroResponse> tratarFalhaPublicacao(PublicacaoEventoException ex) {
        log.error("Falha ao publicar evento de pedido no RabbitMQ", ex);

        ErroResponse corpo = new ErroResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "Falha ao publicar o evento de pedido no broker de mensageria",
                List.of()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(corpo);
    }

    private static ErroCampo paraErroCampo(FieldError fieldError) {
        return new ErroCampo(fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * Formato padrão de resposta de erro da API.
     *
     * @param timestamp momento em que o erro foi tratado
     * @param status    código de status HTTP
     * @param mensagem  descrição legível do erro
     * @param campos    lista de campos inválidos (vazia quando não aplicável)
     */
    public record ErroResponse(
            Instant timestamp,
            int status,
            String mensagem,
            List<ErroCampo> campos
    ) {
    }

    /**
     * Detalhe de um campo inválido do corpo da requisição.
     *
     * @param campo    nome do campo
     * @param mensagem mensagem de validação associada
     */
    public record ErroCampo(String campo, String mensagem) {
    }
}
