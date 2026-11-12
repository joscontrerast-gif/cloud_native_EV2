package com.lumina.bodega.config;

import com.lumina.bodega.entity.Inventario;
import com.lumina.bodega.repository.InventarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crea el inventario inicial de bodega para los productos sembrados por ms-productos.
 * Los IDs 1..12 corresponden al orden del DataSeeder de ms-productos (BD recién creada).
 * Solo inserta los productos que todavía no tienen inventario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventarioSeeder implements CommandLineRunner {

    private final InventarioRepository inventarioRepository;

    private record Item(long productoId, String nombre, int stock) {}

    private static final List<Item> ITEMS = List.of(
            new Item(1, "Monitor Samsung 24 pulgadas", 15),
            new Item(2, "Monitor Samsung 27 pulgadas", 8),
            new Item(3, "Teclado Logitech K380", 25),
            new Item(4, "Mouse Logitech M185", 30),
            new Item(5, "Webcam Logitech C270", 12),
            new Item(6, "Notebook Lenovo IdeaPad 3", 7),
            new Item(7, "Notebook Lenovo ThinkPad", 5),
            new Item(8, "Audifonos Sony WH-CH520", 20),
            new Item(9, "Parlante Sony SRS-XB100", 14),
            new Item(10, "SSD Kingston NV2 1TB", 18),
            new Item(11, "Memoria Kingston Fury 16GB", 22),
            new Item(12, "SSD Kingston XS1000 1TB", 10)
    );

    @Override
    public void run(String... args) {
        int creados = 0;
        for (Item item : ITEMS) {
            if (inventarioRepository.findByProductoId(item.productoId()).isEmpty()) {
                inventarioRepository.save(Inventario.builder()
                        .productoId(item.productoId())
                        .nombreProducto(item.nombre())
                        .stock(item.stock())
                        .stockMinimo(5)
                        .ubicacionBodega("Bodega Principal")
                        .build());
                creados++;
            }
        }
        log.info("InventarioSeeder: {} registros de inventario creados.", creados);
    }
}