package com.lumina.pagos.service;

import com.lumina.pagos.client.MercadoPagoClient;
import com.lumina.pagos.client.MercadoPagoPreferenceResult;
import com.lumina.pagos.dto.*;
import com.lumina.pagos.event.PagoEventPublisher;
import com.lumina.pagos.event.PagoExitosoEvent;
import com.lumina.pagos.event.PedidoSqsPublisher;
import com.lumina.pagos.exception.PagoNotFoundException;
import com.lumina.pagos.model.EstadoPago;
import com.lumina.pagos.model.Pago;
import com.lumina.pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final PagoEventPublisher eventPublisher;
    private final PedidoSqsPublisher pedidoSqsPublisher;

    @Transactional
    public PreferenciaResponse crearPreferencia(CrearPreferenciaRequest request) {
        BigDecimal montoTotal = request.getItems().stream()
                .map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pago pago = Pago.builder()
                .ordenId(request.getOrdenId())
                .usuarioId(request.getUsuarioId())
                .monto(montoTotal)
                .moneda("CLP")
                .estado(EstadoPago.PENDIENTE)
                .descripcion("Orden #" + request.getOrdenId())
                .build();

        pago = pagoRepository.save(pago);

        MercadoPagoPreferenceResult mpResult = mercadoPagoClient.crearPreferencia(request, pago.getId());

        pago.setMpPreferenceId(mpResult.getPreferenceId());
        pago.setMpInitPoint(mpResult.getInitPoint());
        pago = pagoRepository.save(pago);

        log.info("Pago creado y preferencia generada: pagoId={}, ordenId={}, mpPreferenceId={}",
                pago.getId(), pago.getOrdenId(), mpResult.getPreferenceId());

        return PreferenciaResponse.builder()
                .pagoId(pago.getId())
                .ordenId(pago.getOrdenId())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .estado(pago.getEstado())
                .mpPreferenceId(mpResult.getPreferenceId())
                .initPoint(mpResult.getInitPoint())
                .sandboxInitPoint(mpResult.getSandboxInitPoint())
                .build();
    }

    @Transactional
    public PagoResponse simularPagoAprobado(CrearPreferenciaRequest request) {
        BigDecimal montoTotal = request.getItems().stream()
                .map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String simulatedPaymentId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Pago pago = Pago.builder()
                .ordenId(request.getOrdenId())
                .usuarioId(request.getUsuarioId())
                .monto(montoTotal)
                .moneda("CLP")
                .estado(EstadoPago.APROBADO)
                .mpPaymentId(simulatedPaymentId)
                .mpStatus("approved")
                .mpStatusDetail("accredited_simulation")
                .metodoPago("simulacion_demo")
                .descripcion("Simulación Orden #" + request.getOrdenId())
                .fechaPago(LocalDateTime.now())
                .build();

        pago = pagoRepository.save(pago);
        log.info("Pago simulado aprobado: pagoId={}, ordenId={}, mpPaymentId={}",
                pago.getId(), pago.getOrdenId(), simulatedPaymentId);

        List<PedidoPagadoDto.ItemPedidoDto> itemsDto = request.getItems().stream()
                .map(item -> PedidoPagadoDto.ItemPedidoDto.builder()
                        .productoId(item.getProductoId())
                        .nombreProducto(item.getTitulo())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .build())
                .toList();

        pedidoSqsPublisher.publicarPedidoPagado(PedidoPagadoDto.builder()
                .ordenId(pago.getOrdenId())
                .usuarioId(pago.getUsuarioId())
                .montoTotal(pago.getMonto())
                .estado("PAGADO")
                .items(itemsDto)
                .build());

        eventPublisher.publicarPagoExitoso(PagoExitosoEvent.builder()
                .pagoId(pago.getId())
                .ordenId(pago.getOrdenId())
                .usuarioId(pago.getUsuarioId())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .mpPaymentId(simulatedPaymentId)
                .metodoPago("simulacion_demo")
                .fechaPago(LocalDateTime.now())
                .build());

        return mapToResponse(pago);
    }

    @Transactional
    public void procesarWebhookPago(String mpPaymentId) {
        if (mpPaymentId == null || mpPaymentId.isBlank()) {
            return;
        }

        var existingByPaymentId = pagoRepository.findByMpPaymentId(mpPaymentId);
        if (existingByPaymentId.isPresent() && existingByPaymentId.get().getEstado() == EstadoPago.APROBADO) {
            log.info("Pago ya procesado anteriormente (idempotencia): mpPaymentId={}", mpPaymentId);
            return;
        }

        var paymentInfoOpt = mercadoPagoClient.obtenerPago(mpPaymentId);
        if (paymentInfoOpt.isEmpty()) {
            log.warn("No se pudo obtener informacion del pago desde MP: mpPaymentId={}", mpPaymentId);
            return;
        }

        var paymentInfo = paymentInfoOpt.get();
        String externalRef = paymentInfo.getExternalReference();
        if (externalRef == null || externalRef.isBlank()) {
            log.warn("Pago de MP sin external_reference: mpPaymentId={}", mpPaymentId);
            return;
        }

        Pago pago = pagoRepository.findByOrdenId(externalRef)
                .orElseGet(() -> pagoRepository.findById(Long.parseLong(externalRef))
                        .orElseThrow(() -> new PagoNotFoundException("Pago no encontrado para referencia: " + externalRef)));

        if (pago.getEstado() == EstadoPago.APROBADO) {
            log.info("Pago ya estaba aprobado: pagoId={}", pago.getId());
            return;
        }

        pago.setMpPaymentId(mpPaymentId);
        pago.setMpStatus(paymentInfo.getStatus());
        pago.setMpStatusDetail(paymentInfo.getStatusDetail());
        pago.setMetodoPago(paymentInfo.getPaymentMethodId());

        switch (paymentInfo.getStatus()) {
            case "approved" -> {
                pago.setEstado(EstadoPago.APROBADO);
                pago.setFechaPago(LocalDateTime.now());
                pagoRepository.save(pago);

                log.info("Pago APROBADO: pagoId={}, ordenId={}, mpPaymentId={}",
                        pago.getId(), pago.getOrdenId(), mpPaymentId);

                eventPublisher.publicarPagoExitoso(PagoExitosoEvent.builder()
                        .pagoId(pago.getId())
                        .ordenId(pago.getOrdenId())
                        .usuarioId(pago.getUsuarioId())
                        .monto(pago.getMonto())
                        .moneda(pago.getMoneda())
                        .mpPaymentId(mpPaymentId)
                        .metodoPago(paymentInfo.getPaymentMethodId())
                        .fechaPago(LocalDateTime.now())
                        .build());

                pedidoSqsPublisher.publicarPedidoPagado(PedidoPagadoDto.builder()
                        .ordenId(pago.getOrdenId())
                        .usuarioId(pago.getUsuarioId())
                        .montoTotal(pago.getMonto())
                        .estado("PAGADO")
                        .build());
            }
            case "rejected" -> {
                pago.setEstado(EstadoPago.RECHAZADO);
                pagoRepository.save(pago);
                log.info("Pago RECHAZADO: pagoId={}, ordenId={}, detail={}",
                        pago.getId(), pago.getOrdenId(), paymentInfo.getStatusDetail());
            }
            case "cancelled" -> {
                pago.setEstado(EstadoPago.CANCELADO);
                pagoRepository.save(pago);
                log.info("Pago CANCELADO: pagoId={}, ordenId={}", pago.getId(), pago.getOrdenId());
            }
            default -> {
                pago.setEstado(EstadoPago.EN_PROCESO);
                pagoRepository.save(pago);
                log.info("Pago en estado intermedio ({}): pagoId={}", paymentInfo.getStatus(), pago.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPago(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new PagoNotFoundException("Pago no encontrado con ID: " + id));
        return mapToResponse(pago);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPagoPorOrden(String ordenId) {
        Pago pago = pagoRepository.findByOrdenId(ordenId)
                .orElseThrow(() -> new PagoNotFoundException("Pago no encontrado para orden: " + ordenId));
        return mapToResponse(pago);
    }

    private PagoResponse mapToResponse(Pago pago) {
        return PagoResponse.builder()
                .id(pago.getId())
                .ordenId(pago.getOrdenId())
                .usuarioId(pago.getUsuarioId())
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .estado(pago.getEstado())
                .mpPreferenceId(pago.getMpPreferenceId())
                .mpPaymentId(pago.getMpPaymentId())
                .mpStatus(pago.getMpStatus())
                .mpStatusDetail(pago.getMpStatusDetail())
                .metodoPago(pago.getMetodoPago())
                .descripcion(pago.getDescripcion())
                .fechaPago(pago.getFechaPago())
                .creadoEn(pago.getCreadoEn())
                .build();
    }
}