package com.maamacrea.backend.productos;

import com.maamacrea.backend.insumos.Insumo;
import com.maamacrea.backend.insumos.InsumoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private static final BigDecimal MULTIPLICADOR_PRECIO_SUGERIDO = new BigDecimal("3.5");

    private final ProductoRepository productoRepository;
    private final ProductoInsumoRepository productoInsumoRepository;
    private final InsumoRepository insumoRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            ProductoInsumoRepository productoInsumoRepository,
            InsumoRepository insumoRepository) {
        this.productoRepository = productoRepository;
        this.productoInsumoRepository = productoInsumoRepository;
        this.insumoRepository = insumoRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    @Transactional
    public Producto guardar(Producto producto) {
        Producto guardado = productoRepository.save(producto);
        return recalcularYGuardar(guardado);
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }

    @Transactional
    public Producto calcularCostos(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        return recalcularYGuardar(producto);
    }

    @Transactional
    public ProductoInsumo agregarInsumo(Long productoId, Long insumoId, BigDecimal cantidadUsada) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: " + insumoId));

        ProductoInsumo productoInsumo = new ProductoInsumo();
        productoInsumo.setProducto(producto);
        productoInsumo.setInsumo(insumo);
        productoInsumo.setCantidadUsada(cantidadUsada);

        ProductoInsumo guardado = productoInsumoRepository.save(productoInsumo);
        recalcularYGuardar(producto);
        return guardado;
    }

    private Producto recalcularYGuardar(Producto producto) {
        List<ProductoInsumo> relaciones = productoInsumoRepository.findByProductoId(producto.getId());
        BigDecimal costoMateriales = relaciones.stream()
                .map(this::calcularCostoRelacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal precioVenta = producto.getPrecioVenta() == null
                ? BigDecimal.ZERO
                : producto.getPrecioVenta().setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioSugerido = costoMateriales
                .multiply(MULTIPLICADOR_PRECIO_SUGERIDO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ganancia = precioVenta.subtract(costoMateriales).setScale(2, RoundingMode.HALF_UP);

        producto.setPrecioVenta(precioVenta);
        producto.setCostoMateriales(costoMateriales);
        producto.setPrecioSugerido(precioSugerido);
        producto.setGanancia(ganancia);
        return productoRepository.save(producto);
    }

    private BigDecimal calcularCostoRelacion(ProductoInsumo productoInsumo) {
        Insumo insumo = productoInsumo.getInsumo();
        BigDecimal precioCompra = valorOZero(insumo.getPrecioCompra());
        BigDecimal cantidadComprada = valorOZero(insumo.getCantidadComprada());
        BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());

        if (cantidadComprada.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal precioUnitario = precioCompra.divide(cantidadComprada, 8, RoundingMode.HALF_UP);
        return precioUnitario.multiply(cantidadUsada);
    }

    private BigDecimal valorOZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
