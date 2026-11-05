
# Tienda Retail Lumina - Microservicios

Plataforma de retail migrada de monolito a **Arquitectura de Microservicios** para resolver problemas de saturacion en horas peak.

## Arquitectura

```
                    ┌─────────────┐
                    │   Frontend   │  React + Vite + Tailwind
                    │  :5173       │  + Lucide + Framer Motion
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ BFF Gateway │  Spring Boot :8080
                    └──────┬──────┘
           ┌───────┬───────┼───────┬───────┬───────┐
           │       │       │       │       │       │
      ┌────▼─┐ ┌──▼───┐ ┌─▼──┐ ┌─▼──┐ ┌──▼──┐ ┌─▼──┐
      │Users │ │Prods │ │Bod.│ │Cart│ │Deliv│ │Pago│
      │:8081 │ │:8082 │ │:8083│ │:8084│ │:8085│ │:8086│
      └──┬───┘ └──┬───┘ └─┬──┘ └─┬──┘ └──┬──┘ └─┬──┘
         │        │       │      │       │      │
      ┌──▼──┐ ┌──▼──┐ ┌──▼─┐ ┌─▼──┐ ┌──▼─┐ ┌─▼──┐
      │MySQL│ │MySQL│ │MySQL│ │MySQL│ │MySQL│ │MySQL│
      │:3307│ │:3308│ │:3309│ │:3310│ │:3311│ │:3312│
      └─────┘ └─────┘ └────┘ └────┘ └────┘ └────┘
```

## Stack Tecnologico

| Capa | Tecnologia |
|------|-----------|
| Frontend | React, Vite, Tailwind CSS, Lucide React, Framer Motion |
| Backend | Java 17, Spring Boot 3.2, JPA/Hibernate |
| Base de Datos | MySQL 8.0 (una instancia por servicio) |
| Pagos | MercadoPago SDK |
| Infraestructura | Docker, Docker Compose |

## Microservicios

| Servicio | Puerto | BD Puerto | Descripcion |
|----------|--------|-----------|-------------|
| BFF Gateway | 8080 | - | API Gateway / Backend For Frontend |
| ms-usuarios | 8081 | 3307 | Perfiles, autenticacion JWT |
| ms-productos | 8082 | 3308 | Catalogo y marcas |
| ms-bodega | 8083 | 3309 | Inventario y alertas de stock |
| ms-carrito | 8084 | 3310 | Carrito de compras |
| ms-delivery | 8085 | 3311 | Seguimiento de entregas |
| ms-pagos | 8086 | 3312 | Integracion MercadoPago |

## Requisitos Previos

- **Docker Desktop** instalado y corriendo
- **Node.js** (v18 o superior) para el frontend

## Inicio Rapido (desde cero)

### Paso 1: Abrir Docker Desktop

Asegurate de que Docker Desktop este abierto y corriendo. Puedes verificar con:

```bash
docker ps
```

Si no muestra error, Docker esta listo.

### Paso 2: Levantar todos los servicios backend

Desde la raiz del proyecto:

```bash
docker-compose up -d --build
```

Esto levanta automaticamente:
- 6 bases de datos MySQL
- 6 microservicios Java
- 1 BFF Gateway

Espera ~1-2 minutos la primera vez (descarga imagenes y compila). Verifica que todo este corriendo:

```bash
docker ps
```

Deberias ver 13 contenedores corriendo.

### Paso 3: Levantar el frontend

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

### Paso 4: Abrir la aplicacion

- **Tienda:** http://localhost:5173
- **API Gateway:** http://localhost:8080
- **ADMIN MQ:**   

### Credenciales de Admin

| Campo | Valor |
|-------|-------|
| Email | admin@lumina.cl |
| Password | admin123 |

### Credenciales de Usuario Prueba(Recomendado)

| Campo | Valor |
|-------|-------|
| Email | user@lumina.cl |
| Password | user1234 |



## Comandos Utiles

```bash
# Ver logs de un servicio especifico
docker-compose logs -f ms-productos

# Detener todos los servicios
docker-compose down

# Detener solo un servicio
docker-compose stop ms-productos

# Reiniciar con rebuild (despues de cambios en backend)
docker-compose up -d --build

# Ver estado de contenedores
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

## Panel de Administracion

Accede con la cuenta admin para:
- **Productos**: Crear, editar y eliminar productos del catalogo
- **Bodega**: Ver inventario, ajustar stock (agregar/descontar) y ver alertas de stock critico


# cloud_native-
7295d3e95695c368e0f4e8503cea6a99ce6516e8
