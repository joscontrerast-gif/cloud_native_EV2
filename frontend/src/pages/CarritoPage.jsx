import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { pagosService, bodegaService } from '../services/api';
import { formatCLP } from '../utils/format';

export default function CarritoPage() {
  const { items, updateQuantity, removeItem, clearCart, totalPrice, totalItems } = useCart();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [pagando, setPagando] = useState(false);
  const [error, setError] = useState('');

  const handlePagar = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setPagando(true);
    setError('');
    try {
      const ordenId = 'ORD-' + Date.now();
      const payload = {
        ordenId,
        usuarioId: user?.id || 1,
        emailComprador: user?.email,
        items: items.map((item) => ({
          titulo: item.nombre,
          productoId: item.id,
          cantidad: item.cantidad,
          precioUnitario: Number(item.precio),
          descripcion: item.marca || 'Producto',
          imagenUrl: item.imagen || '',
        })),
      };
      const { data } = await pagosService.crearPreferencia(payload);
      localStorage.setItem('lumina-last-orden', ordenId);
      const url = data.sandboxInitPoint || data.initPoint;
      if (url) {
        window.location.href = url;
      } else {
        setError('No se obtuvo URL de pago');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Error al iniciar el pago con Mercado Pago');
    } finally {
      setPagando(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
          </svg>
        </div>
        <h2 className="text-2xl font-semibold text-slate-900 mb-2">Tu carrito está vacío</h2>
        <p className="text-slate-500 text-sm mb-6">Explora nuestro catálogo y agrega productos para comenzar</p>
        <Link to="/" className="btn-primary inline-block">Ir a la tienda</Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Carrito de compras ({totalItems})</h1>
        <button onClick={clearCart} className="text-xs text-slate-500 hover:text-red-600 transition">
          Vaciar carrito
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-3">
          {items.map((item) => (
            <div key={item.id} className="card p-4 flex items-center gap-4">
              <img
                src={item.imagen}
                alt={item.nombre}
                className="w-20 h-20 object-cover rounded-lg bg-slate-100 flex-shrink-0"
              />
              <div className="flex-1 min-w-0">
                <p className="text-xs text-slate-400">{item.marca}</p>
                <h3 className="font-semibold text-slate-900 text-sm truncate">{item.nombre}</h3>
                <p className="text-sm font-medium text-slate-700 mt-1">{formatCLP(item.precio)}</p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => updateQuantity(item.id, item.cantidad - 1)}
                  className="w-7 h-7 rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 text-sm"
                >
                  -
                </button>
                <span className="w-7 text-center text-sm font-medium">{item.cantidad}</span>
                <button
                  onClick={() => updateQuantity(item.id, item.cantidad + 1)}
                  className="w-7 h-7 rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 text-sm"
                >
                  +
                </button>
              </div>
              <div className="text-right min-w-[90px]">
                <p className="font-semibold text-slate-900 text-sm">
                  {formatCLP(item.precio * item.cantidad)}
                </p>
                <button
                  onClick={() => removeItem(item.id)}
                  className="text-xs text-slate-400 hover:text-red-600 mt-1"
                >
                  Quitar
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="card p-6 h-fit">
          <h2 className="font-semibold text-slate-900 mb-4">Resumen de compra</h2>
          <div className="space-y-2 text-sm border-b border-slate-200 pb-4 mb-4">
            <div className="flex justify-between text-slate-600">
              <span>Productos ({totalItems})</span>
              <span>{formatCLP(totalPrice)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Envío</span>
              <span className="text-emerald-600 font-medium">Gratis</span>
            </div>
          </div>
          <div className="flex justify-between items-baseline mb-6">
            <span className="font-semibold text-slate-900">Total</span>
            <span className="text-xl font-bold text-slate-900">{formatCLP(totalPrice)}</span>
          </div>

          <button
            onClick={handlePagar}
            disabled={pagando}
            className="btn-primary w-full flex items-center justify-center gap-2 mb-3"
          >
            {pagando ? 'Conectando con Mercado Pago...' : 'Pagar con Mercado Pago'}
          </button>

          <button
            onClick={async () => {
              if (!isAuthenticated) {
                navigate('/login');
                return;
              }
              setPagando(true);
              setError('');
              try {
                const demoOrdenId = 'ORD-DEMO-' + Date.now();
                const payload = {
                  ordenId: demoOrdenId,
                  usuarioId: user?.id || 1,
                  emailComprador: user?.email,
                  items: items.map((item) => ({
                    titulo: item.nombre,
                    productoId: item.id,
                    cantidad: item.cantidad,
                    precioUnitario: Number(item.precio),
                    descripcion: item.marca || 'Producto',
                    imagenUrl: item.imagen || '',
                  })),
                };

                await pagosService.simularPago(payload);

                const pedido = {
                  ordenId: demoOrdenId,
                  estado: 'reparto',
                  timestamp: Date.now(),
                  items: items.length,
                  total: totalPrice,
                };
                const pedidosKey = `lumina-pedidos-${user?.id || 'guest'}`;
                const pedidos = JSON.parse(localStorage.getItem(pedidosKey) || '[]');
                pedidos.push(pedido);
                localStorage.setItem(pedidosKey, JSON.stringify(pedidos));
                localStorage.setItem('lumina-last-orden', demoOrdenId);

                clearCart();
                navigate('/pago/exitoso');
              } catch (err) {
                setError(err.response?.data?.message || 'Error al simular el pago en ms-pagos');
              } finally {
                setPagando(false);
              }
            }}
            disabled={pagando}
            className="btn-secondary w-full text-xs"
          >
            {pagando ? 'Procesando simulación...' : 'Simular Pago (Demo)'}
          </button>

          {!isAuthenticated && (
            <p className="text-xs text-slate-500 text-center mt-3">
              Necesitas <Link to="/login" className="text-accent-600 font-medium">iniciar sesión</Link> para pagar
            </p>
          )}
        </div>
      </div>
    </div>
  );
}