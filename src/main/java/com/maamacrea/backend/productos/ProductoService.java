package com.maamacrea.backend.productos;

import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.insumos.Insumo;
import com.maamacrea.backend.insumos.InsumoCompra;
import com.maamacrea.backend.insumos.InsumoCompraRepository;
import com.maamacrea.backend.insumos.InsumoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private static final BigDecimal MULTIPLICADOR_PRECIO_SUGERIDO = new BigDecimal("3.5");

    private final ProductoRepository productoRepository;
    private final ProductoInsumoRepository productoInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final InsumoCompraRepository insumoCompraRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            ProductoInsumoRepository productoInsumoRepository,
            InsumoRepository insumoRepository,
            InsumoCompraRepository insumoCompraRepository) {
        this.productoRepository = productoRepository;
        this.productoInsumoRepository = productoInsumoRepository;
        this.insumoRepository = insumoRepository;
        this.insumoCompraRepository = insumoCompraRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResumenResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .map(producto -> new ProductoResumenResponse(
                        producto.getId(),
                        producto.getCodigo(),
                        producto.getNombre(),
                        producto.getTipoProducto(),
                        productoInsumoRepository.findByProductoId(producto.getId()).size(),
                        producto.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        Producto producto = obtenerProducto(id);
        return toProductoResponse(producto);
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoCreateRequest request) {
        validarProductoRequest(request);
        if (productoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new IllegalArgumentException("Ya existe un producto con el codigo indicado.");
        }

        Producto producto = new Producto();
        producto.setCodigo(request.codigo().trim());
        producto.setNombre(request.nombre().trim());
        producto.setTipoProducto(request.tipoProducto());

        Producto guardado = productoRepository.save(producto);
        guardarRelaciones(guardado, request.insumos());
        return toProductoResponse(recalcularYGuardar(guardado));
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long productoId, ProductoCreateRequest request) {
        validarProductoRequest(request);
        Producto producto = obtenerProducto(productoId);

        String codigoNormalizado = request.codigo().trim();
        if (!producto.getCodigo().equalsIgnoreCase(codigoNormalizado)
                && productoRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new IllegalArgumentException("Ya existe un producto con el codigo indicado.");
        }

        producto.setCodigo(codigoNormalizado);
        producto.setNombre(request.nombre().trim());
        producto.setTipoProducto(request.tipoProducto());

        Producto guardado = productoRepository.save(producto);
        productoInsumoRepository.deleteByProductoId(productoId);
        guardarRelaciones(guardado, request.insumos());
        return toProductoResponse(recalcularYGuardar(guardado));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = obtenerProducto(id);
        productoInsumoRepository.deleteByProductoId(producto.getId());
        productoRepository.delete(producto);
    }

    @Transactional
    public ProductoResponse calcularCostos(Long productoId) {
        Producto producto = obtenerProducto(productoId);
        return toProductoResponse(recalcularYGuardar(producto));
    }

    @Transactional
    public ProductoInsumoResponse agregarInsumo(Long productoId, ProductoInsumoCreateRequest request) {
        Producto producto = obtenerProducto(productoId);
        validarProductoInsumoRequest(request);
        Insumo insumo = obtenerInsumo(request.insumoId());

        ProductoInsumo productoInsumo = construirRelacion(producto, insumo, request);
        ProductoInsumo guardado = productoInsumoRepository.save(productoInsumo);
        recalcularYGuardar(producto);
        return toProductoInsumoResponse(guardado);
    }

    private void guardarRelaciones(Producto producto, List<ProductoInsumoCreateRequest> insumos) {
        for (ProductoInsumoCreateRequest item : insumos) {
            validarProductoInsumoRequest(item);
            Insumo insumo = obtenerInsumo(item.insumoId());
            ProductoInsumo relacion = construirRelacion(producto, insumo, item);
            productoInsumoRepository.save(relacion);
        }
    }

    private ProductoInsumo construirRelacion(
            Producto producto,
            Insumo insumo,
            ProductoInsumoCreateRequest request) {
        validarProductoInsumoRequest(request);
        validarMedidaPositiva(request.anchoUsadoCm(), "El ancho usado debe ser mayor a cero.");
        validarMedidaPositiva(request.altoLargoUsadoCm(), "El alto o largo usado debe ser mayor a cero.");

        ProductoInsumo productoInsumo = new ProductoInsumo();
        productoInsumo.setProducto(producto);
        productoInsumo.setInsumo(insumo);
        productoInsumo.setCantidadUsada(request.cantidadUsada().setScale(4, RoundingMode.HALF_UP));
        productoInsumo.setAnchoUsadoCm(scaleOptional(request.anchoUsadoCm(), 3));
        productoInsumo.setAltoLargoUsadoCm(scaleOptional(request.altoLargoUsadoCm(), 3));
        productoInsumo.setConsumo(normalizeOptionalText(request.consumo()));
        productoInsumo.setCostoEstimado(scaleOptional(request.costoEstimado(), 4));
        return productoInsumo;
    }

    private Producto recalcularYGuardar(Producto producto) {
        List<ProductoInsumo> relaciones = productoInsumoRepository.findByProductoIdOrderByIdAsc(producto.getId());
        BigDecimal costoMateriales = relaciones.stream()
                .map(this::calcularCostoRelacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal precioVenta = valorOZero(producto.getPrecioVenta()).setScale(2, RoundingMode.HALF_UP);
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
        Optional<InsumoCompra> compraVigente = obtenerCompraVigente(insumo.getId());
        if (compraVigente.isPresent()) {
            BigDecimal costoDesdeCompra = calcularCostoDesdeCompraVigente(productoInsumo, compraVigente.get());
            if (costoDesdeCompra != null) {
                return costoDesdeCompra.setScale(4, RoundingMode.HALF_UP);
            }
        }

        if (productoInsumo.getCostoEstimado() != null) {
            return productoInsumo.getCostoEstimado().setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal costoUnitario = valorOZero(insumo.getCostoUnitario());
        BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());

        if (costoUnitario.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal precioCompra = valorOZero(insumo.getPrecioCompra());
            BigDecimal cantidadComprada = valorOZero(insumo.getCantidadComprada());
            if (cantidadComprada.compareTo(BigDecimal.ZERO) == 0 || precioCompra.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            costoUnitario = precioCompra.divide(cantidadComprada, 8, RoundingMode.HALF_UP);
        }

        return costoUnitario.multiply(cantidadUsada);
    }

    private BigDecimal calcularCostoDesdeCompraVigente(ProductoInsumo productoInsumo, InsumoCompra compraVigente) {
        BigDecimal precioCompraTotal = valorOZero(compraVigente.getPrecioCompraTotal());
        if (precioCompraTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());
        BigDecimal anchoCompra = compraVigente.getAncho();
        BigDecimal altoCompra = compraVigente.getAlto();
        BigDecimal anchoUsado = productoInsumo.getAnchoUsadoCm();
        BigDecimal altoUsado = productoInsumo.getAltoLargoUsadoCm();

        if (anchoCompra != null && altoCompra != null && anchoUsado != null && altoUsado != null) {
            BigDecimal areaTotalComprada = anchoCompra.multiply(altoCompra);
            if (areaTotalComprada.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal costoPorCm2 = precioCompraTotal.divide(areaTotalComprada, 8, RoundingMode.HALF_UP);
                BigDecimal areaUsada = anchoUsado.multiply(altoUsado).multiply(cantidadUsada);
                return costoPorCm2.multiply(areaUsada);
            }
        }

        BigDecimal precioUnitario = valorOZero(compraVigente.getPrecioUnitario());
        if (precioUnitario.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal cantidadComprada = valorOZero(compraVigente.getCantidadComprada());
            if (cantidadComprada.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }
            precioUnitario = precioCompraTotal.divide(cantidadComprada, 8, RoundingMode.HALF_UP);
        }

        return precioUnitario.multiply(cantidadUsada);
    }

    private ProductoResponse toProductoResponse(Producto producto) {
        List<ProductoInsumo> relaciones = productoInsumoRepository.findByProductoIdOrderByIdAsc(producto.getId());
        List<ProductoInsumoResponse> insumos = relaciones.stream().map(this::toProductoInsumoResponse).toList();
        List<ProductoCambioPrecioResponse> cambiosPrecio = construirCambiosPrecio(relaciones);
        LocalDate ultimoCambioPrecio = cambiosPrecio.stream()
                .map(ProductoCambioPrecioResponse::ultimoCambioPrecio)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getTipoProducto(),
                producto.getCreatedAt(),
                producto.getPrecioVenta(),
                producto.getCostoMateriales(),
                producto.getPrecioSugerido(),
                producto.getGanancia(),
                ultimoCambioPrecio,
                !cambiosPrecio.isEmpty(),
                cambiosPrecio,
                insumos);
    }

    private List<ProductoCambioPrecioResponse> construirCambiosPrecio(List<ProductoInsumo> relaciones) {
        Map<Long, ProductoCambioPrecioResponse> cambiosPorInsumo = new LinkedHashMap<>();

        for (ProductoInsumo relacion : relaciones) {
            Insumo insumo = relacion.getInsumo();
            if (cambiosPorInsumo.containsKey(insumo.getId())) {
                continue;
            }

            List<InsumoCompra> compras = insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumo.getId());
            if (compras == null || compras.size() < 2) {
                continue;
            }

            InsumoCompra vigente = obtenerCompraVigente(insumo.getId()).orElse(compras.get(0));
            InsumoCompra anterior = compras.stream()
                    .filter(compra -> !Objects.equals(compra.getId(), vigente.getId()))
                    .findFirst()
                    .orElse(null);

            if (anterior == null || sameBigDecimal(vigente.getPrecioCompraTotal(), anterior.getPrecioCompraTotal())) {
                continue;
            }

            cambiosPorInsumo.put(
                    insumo.getId(),
                    new ProductoCambioPrecioResponse(
                            insumo.getId(),
                            insumo.getCodigoProducto(),
                            insumo.getNombre(),
                            vigente.getFechaCompra(),
                            anterior.getPrecioCompraTotal(),
                            vigente.getPrecioCompraTotal(),
                            calcularVariacionPorcentual(
                                    anterior.getPrecioCompraTotal(), vigente.getPrecioCompraTotal())));
        }

        return cambiosPorInsumo.values().stream()
                .sorted((left, right) -> right.ultimoCambioPrecio().compareTo(left.ultimoCambioPrecio()))
                .toList();
    }

    private Optional<InsumoCompra> obtenerCompraVigente(Long insumoId) {
        Optional<InsumoCompra> vigente =
                insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        if (vigente != null && vigente.isPresent()) {
            return vigente;
        }

        Optional<InsumoCompra> ultima =
                insumoCompraRepository.findFirstByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        return ultima == null ? Optional.empty() : ultima;
    }

    private ProductoInsumoResponse toProductoInsumoResponse(ProductoInsumo productoInsumo) {
        Insumo insumo = productoInsumo.getInsumo();
        return new ProductoInsumoResponse(
                productoInsumo.getId(),
                insumo.getId(),
                insumo.getCodigoProducto(),
                insumo.getNombre(),
                insumo.getCategoria(),
                insumo.getUnidadMedida(),
                productoInsumo.getCantidadUsada(),
                productoInsumo.getAnchoUsadoCm(),
                productoInsumo.getAltoLargoUsadoCm(),
                buildMedidaUsadaTexto(productoInsumo),
                defaultConsumo(productoInsumo.getConsumo()),
                productoInsumo.getCostoEstimado(),
                productoInsumo.getCreatedAt());
    }

    private String buildMedidaUsadaTexto(ProductoInsumo productoInsumo) {
        if (productoInsumo.getAnchoUsadoCm() == null || productoInsumo.getAltoLargoUsadoCm() == null) {
            return "No aplica";
        }

        return productoInsumo.getAnchoUsadoCm().stripTrailingZeros().toPlainString()
                + " x "
                + productoInsumo.getAltoLargoUsadoCm().stripTrailingZeros().toPlainString()
                + " cm";
    }

    private String defaultConsumo(String consumo) {
        String normalized = normalizeOptionalText(consumo);
        return normalized == null ? "No aplica" : normalized;
    }

    private void validarProductoRequest(ProductoCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud del producto es obligatoria.");
        }
        if (request.codigo() == null || request.codigo().isBlank()) {
            throw new IllegalArgumentException("El codigo del producto es obligatorio.");
        }
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        if (request.tipoProducto() == null) {
            throw new IllegalArgumentException("El tipo de producto es obligatorio.");
        }
        if (request.insumos() == null || request.insumos().isEmpty()) {
            throw new IllegalArgumentException("Debes asociar al menos un insumo al producto.");
        }
    }

    private void validarProductoInsumoRequest(ProductoInsumoCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El detalle del insumo es obligatorio.");
        }
        if (request.insumoId() == null) {
            throw new IllegalArgumentException("El insumo es obligatorio.");
        }
        if (request.cantidadUsada() == null || request.cantidadUsada().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad usada debe ser mayor a cero.");
        }
        if (request.costoEstimado() != null && request.costoEstimado().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo estimado no puede ser negativo.");
        }
    }

    private Producto obtenerProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
    }

    private Insumo obtenerInsumo(Long insumoId) {
        return insumoRepository.findById(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado: " + insumoId));
    }

    private BigDecimal calcularVariacionPorcentual(BigDecimal precioAnterior, BigDecimal precioActual) {
        if (precioAnterior == null || precioActual == null || precioAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return precioActual.subtract(precioAnterior)
                .multiply(new BigDecimal("100"))
                .divide(precioAnterior, 2, RoundingMode.HALF_UP);
    }

    private boolean sameBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private void validarMedidaPositiva(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal scaleOptional(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal valorOZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
