package com.lumina.pagos.controller;

import com.lumina.pagos.dto.CrearPreferenciaRequest;
import com.lumina.pagos.dto.PagoResponse;
import com.lumina.pagos.dto.PreferenciaResponse;
import com.lumina.pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Gestion de pagos y preferencias Mercado Pago")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/crear-preferencia")
    @Operation(summary = "Crear preferencia de pago en Mercado Pago")
    public ResponseEntity<PreferenciaResponse> crearPreferencia(
            @Valid @RequestBody CrearPreferenciaRequest request) {
        PreferenciaResponse response = pagoService.crearPreferencia(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/simular-pago")
    @Operation(summary = "Simular un pago aprobado y publicar eventos en SQS y RabbitMQ")
    public ResponseEntity<PagoResponse> simularPago(
            @Valid @RequestBody CrearPreferenciaRequest request) {
        PagoResponse response = pagoService.simularPagoAprobado(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pago por ID")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPago(id));
    }

    @GetMapping("/orden/{ordenId}")
    @Operation(summary = "Obtener pago por ID de orden")
    public ResponseEntity<PagoResponse> obtenerPagoPorOrden(@PathVariable String ordenId) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorOrden(ordenId));
    }
}