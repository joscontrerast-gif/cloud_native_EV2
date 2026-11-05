package com.lumina.pagos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPreferenciaRequest {

    @NotBlank(message = "El ID de orden es obligatorio")
    private String ordenId;

    @NotNull(message = "El ID de usuario es obligatorio")
    private String usuarioId;

    @NotNull(message = "Los items son obligatorios")
    private List<ItemRequest> items;

    private String emailComprador;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {

        @NotBlank(message = "El título del item es obligatorio")
        private String titulo;

        /**
         * Id del producto en el catalogo. Necesario para que ms-bodega descuente el stock
         * al recibir el evento "pedido pagado" de SQS.
         */
        @Positive(message = "El productoId debe ser mayor a 0")
        private Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        private BigDecimal precioUnitario;

        private String descripcion;
        private String imagenUrl;
    }
}
