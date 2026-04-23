<h1 align="center">Sistema de Gestión de Productos Tecnológicos</h1>

<p align="center">
  Plataforma e-commerce con panel de análisis de inventario en tiempo real
</p>

<p align="center">
  <img src="https://img.shields.io/badge/React-Frontend-61DAFB?style=for-the-badge&logo=react&logoColor=black">
  <img src="https://img.shields.io/badge/Firebase-Backend-FFCA28?style=for-the-badge&logo=firebase&logoColor=black">
  <img src="https://img.shields.io/badge/Auth-Google-DB4437?style=for-the-badge&logo=google&logoColor=white">
  <img src="https://img.shields.io/badge/Status-Activo-2ecc71?style=for-the-badge">
</p>

---

## Vista General

Sistema web diseñado para la gestión y venta de productos tecnológicos. Permite a los usuarios navegar por un catálogo dinámico, realizar compras y administrar sus pedidos de forma eficiente.

Incluye un panel administrativo con métricas en tiempo real y control de acceso basado en roles.

---

## Características Clave

<table>
<tr>
<td width="50%">

### Usuarios
- Exploración de catálogo  
- Carrito de compras  
- Gestión de pedidos  
- Sistema de favoritos  
- Notificaciones en tiempo real  
- Perfil personal  

</td>
<td width="50%">

### Administrador
- Gestión completa de productos  
- Control de inventario  
- Análisis de datos  
- Métricas en tiempo real  
- Panel de estadísticas  

</td>
</tr>
</table>

---

## Tecnologías

<div align="center">

| Frontend | Backend | Auth | Otros |
|----------|--------|------|------|
| React | Firebase | Google Auth | CSS / JS |

</div>

---

## Módulos del Sistema

### Catálogo

```text
• Visualización de productos
• Información detallada (precio, stock, imagen, descripción)
• Rating y reseñas
• Acciones: carrito / favoritos
```
Carrito de Compras

• Gestión dinámica de productos
• Cálculo automático del total
• Métodos de pago:
  - Tarjeta (Visa / Débito)
  - Yape
  - BCP
  - Scotiabank

Pedidos
• Historial de compras
• Detalle de pedidos
• Recompra rápida
• Generación de ticket imprimible

Notificaciones
Tipos:
- Pedidos
- Pagos
- Ofertas
- Sistema

Acciones:
- Filtrar
- Marcar como leídas
- Limpiar historial
Perfil
• Gestión de datos personales
• Dirección de envío
• Información adicional:
  - Fecha de registro
  - Total de compras


Dashboard de Inventario
• Total de productos
• Stock total
• Valor del inventario
• Productos con stock bajo
• Ventas mensuales
• Transacciones semanales
• Distribución por categorías
• Top productos por valor
Roles del Sistema
Usuario
• Explorar productos
• Comprar
• Gestionar carrito
• Ver pedidos
• Administrar favoritos
• Editar perfil
Administrador
Incluye todo lo del usuario +

• Crear / editar / eliminar productos
• Gestionar:
  - Precio
  - Stock
  - Imagen (URL)
  - Descripción
  - Rating


