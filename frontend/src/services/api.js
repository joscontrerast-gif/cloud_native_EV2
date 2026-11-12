import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('lumina-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('lumina-token');
      localStorage.removeItem('lumina-user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (data) => api.post('/auth/register', data),
  me: () => api.get('/auth/me'),
};

export const productosService = {
  listar: () => api.get('/productos'),
  obtener: (id) => api.get(`/productos/${id}`),
  porCategoria: (categoria) => api.get(`/productos/categoria/${categoria}`),
  buscar: (q) => api.get('/productos/buscar', { params: { q } }),
  crear: (data) => api.post('/productos', data),
  actualizar: (id, data) => api.put(`/productos/${id}`, data),
  eliminar: (id) => api.delete(`/productos/${id}`),
};

export const bodegaService = {
  listar: () => api.get('/bodega'),
  porProducto: (productoId) => api.get(`/bodega/producto/${productoId}`),
  stockBajo: () => api.get('/bodega/stock-bajo'),
  crear: (data) => api.post('/bodega', data),
  actualizarStock: (id, cantidad) => api.patch(`/bodega/${id}/stock`, { cantidad }),
  descontarStock: (productoId, cantidad) =>
    api.post('/bodega/descontar', { productoId, cantidad }),
};

export const carritoService = {
  obtener: (usuarioId) => api.get(`/carrito/${usuarioId}`),
  agregar: (usuarioId, item) => api.post(`/carrito/${usuarioId}/items`, item),
  actualizarCantidad: (usuarioId, productoId, cantidad) =>
    api.put(`/carrito/${usuarioId}/items/${productoId}`, { cantidad }),
  eliminarItem: (usuarioId, productoId) =>
    api.delete(`/carrito/${usuarioId}/items/${productoId}`),
  vaciar: (usuarioId) => api.delete(`/carrito/${usuarioId}`),
};

export const pagosService = {
  crearPreferencia: (data) => api.post('/pagos/crear-preferencia', data),
  simularPago: (data) => api.post('/pagos/simular-pago', data),
  obtener: (id) => api.get(`/pagos/${id}`),
  obtenerPorOrden: (ordenId) => api.get(`/pagos/orden/${ordenId}`),
};

export const deliveryService = {
  listar: () => api.get('/delivery'),
  porOrden: (ordenId) => api.get(`/delivery/orden/${ordenId}`),
  porUsuario: (usuarioId) => api.get(`/delivery/usuario/${usuarioId}`),
  crear: (data) => api.post('/delivery', data),
  actualizarEstado: (id, estado, notas) =>
    api.patch(`/delivery/${id}/estado`, { estado, notas }),
};

export default api;