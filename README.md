<h1 align="center">Sistema de Gestión de Productos Tecnológicos</h1>

<p align="center">
  Sistema e-commerce con panel de inventario y análisis en tiempo real
</p>

<p align="center">
  <img src="https://img.shields.io/badge/React-Frontend-blue?style=for-the-badge&logo=react">
  <img src="https://img.shields.io/badge/Firebase-Backend-orange?style=for-the-badge&logo=firebase">
  <img src="https://img.shields.io/badge/Auth-Google-red?style=for-the-badge&logo=google">
  <img src="https://img.shields.io/badge/Status-Activo-success?style=for-the-badge">
</p>

---

## Descripción

Sistema web para la gestión y venta de productos tecnológicos desarrollado con **React** y **Firebase**. Permite a los usuarios explorar productos, realizar compras y gestionar pedidos de forma eficiente.

Incluye un **panel de análisis de inventario** con métricas en tiempo real y un sistema de **roles (usuario / administrador)** para el control de funcionalidades.

---

## Objetivos

- Facilitar la venta de productos tecnológicos en línea  
- Proporcionar control y monitoreo del inventario  
- Mejorar la experiencia del usuario en el proceso de compra  
- Permitir la gestión eficiente de productos  

---

## Stack Tecnológico

| Tecnología | Uso |
|----------|------|
| React (JSX) | Frontend |
| Firebase | Base de datos |
| Firebase Authentication | Autenticación |
| JavaScript | Lógica |
| CSS | Estilos |

---

## Módulos del Sistema

### Catálogo

- Visualización de productos
- Información completa:
  - Precio
  - Stock
  - Imagen (URL)
  - Descripción
  - Rating
- Acciones:
  - Añadir al carrito
  - Añadir a favoritos

---

### Carrito de Compras

- Gestión dinámica de productos
- Cálculo automático del total

**Métodos de pago:**
- Tarjeta (Visa / Débito)
- Yape
- BCP
- Scotiabank

---

### Pedidos

- Historial de compras
- Funcionalidades:
  - Ver detalle
  - Recomprar
  - Generar ticket imprimible

---

### Favoritos

- Lista personalizada de productos
- Acceso rápido a productos marcados

---

### Notificaciones

Sistema en tiempo real:

| Tipo | Descripción |
|------|------------|
| Pedidos | Actualizaciones de compra |
| Pagos | Confirmaciones |
| Ofertas | Promociones |
| Sistema | Eventos generales |

**Acciones disponibles:**
- Filtrar
- Marcar como leídas
- Limpiar

---

### Perfil

Gestión de datos del usuario:

- Foto de perfil
- Información personal
- Dirección de envío

**Extras:**
- Fecha de registro
- Total de compras

---

### Soporte

- Envío de consultas
- Comunicación directa con el sistema

---

### Dashboard de Inventario

Panel con métricas clave:

- Total de productos
- Stock total
- Valor del inventario
- Stock bajo
- Ventas mensuales
- Transacciones semanales
- Distribución por categorías
- Top productos por valor

---

## Roles

### Usuario

- Explorar productos  
- Comprar  
- Gestionar carrito  
- Ver pedidos  
- Favoritos  
- Editar perfil  

---

### Administrador

Incluye todo lo anterior más:

- CRUD de productos
- Gestión de:
  - Precio
  - Stock
  - Imagen (URL)
  - Descripción
  - Rating

---

## Estructura del Proyecto

```bash
src/
├── components/
├── data/
├── hooks/
├── pages/
├── services/
├── firebase.js
├── App.jsx
├── main.jsx
└── index.css
