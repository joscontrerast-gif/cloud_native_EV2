package com.lumina.bff.controller;

import com.lumina.bff.config.ServicesConfig;
import com.lumina.bff.service.CircuitBreakerHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Proxy hacia ms-pagos")
public class PagosProxyController {

    private final WebClient.Builder webClientBuilder;
    private final ServicesConfig config;
    private final CircuitBreakerHelper circuitBreaker;

    @PostMapping("/crear-preferencia")
    @Operation(summary = "Crear preferencia de pago")
    public Mono<ResponseEntity<String>> crearPreferencia(@RequestBody String body) {
        return circuitBreaker.protect("pagos", webClientBuilder.build()
                .post()
                .uri(config.getPagosUrl() + "/api/pagos/crear-preferencia")
                .bodyValue(body)
                .header("Content-Type", "application/json")
                .retrieve()
                .toEntity(String.class)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode())
                                .header("Content-Type", "application/json")
                                .body(ex.getResponseBodyAsString()))));
    }

    @PostMapping("/simular-pago")
    @Operation(summary = "Simular pago aprobado en ms-pagos")
    public Mono<ResponseEntity<String>> simularPago(@RequestBody String body) {
        return circuitBreaker.protect("pagos", webClientBuilder.build()
                .post()
                .uri(config.getPagosUrl() + "/api/pagos/simular-pago")
                .bodyValue(body)
                .header("Content-Type", "application/json")
                .retrieve()
                .toEntity(String.class)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode())
                                .header("Content-Type", "application/json")
                                .body(ex.getResponseBodyAsString()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pago por ID")
    public Mono<ResponseEntity<String>> obtener(@PathVariable Long id) {
        return circuitBreaker.protect("pagos", webClientBuilder.build()
                .get()
                .uri(config.getPagosUrl() + "/api/pagos/" + id)
                .retrieve()
                .toEntity(String.class)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode())
                                .header("Content-Type", "application/json")
                                .body(ex.getResponseBodyAsString()))));
    }

    @GetMapping("/orden/{ordenId}")
    @Operation(summary = "Obtener pago por orden")
    public Mono<ResponseEntity<String>> obtenerPorOrden(@PathVariable String ordenId) {
        return circuitBreaker.protect("pagos", webClientBuilder.build()
                .get()
                .uri(config.getPagosUrl() + "/api/pagos/orden/" + ordenId)
                .retrieve()
                .toEntity(String.class)
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode())
                                .header("Content-Type", "application/json")
                                .body(ex.getResponseBodyAsString()))));
    }
}