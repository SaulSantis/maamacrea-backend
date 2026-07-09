package com.maamacrea.backend.productos;

import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.insumos.Insumo;
import com.maamacrea.backend.insumos.InsumoCompra;
import com.maamacrea.backend.insumos.InsumoCompraRepository;
import com.maamacrea.backend.insumos.InsumoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MARGEN_VENTA_DEFECTO = new BigDecimal("0.50");
    private static final BigDecimal FACTOR_METROS_A_CENTIMETROS = new BigDecimal("100");
    private static final BigDecimal AREA_BASE_TINTA_CM2 = new BigDecimal("1849");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

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

        CosteoDetalleResultado detalle = calcularDetalleRelacion(guardado);
        return toProductoInsumoResponse(guardado, detalle);
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
        CosteoProductoResultado costeo = calcularCosteoProducto(relaciones);

        producto.setCostoMateriales(costeo.costoMateriales());
        producto.setPrecioSugerido(costeo.precioSugerido());
        producto.setGanancia(costeo.gananciaDirecta());

        if (producto.getPrecioVenta() == null) {
            producto.setPrecioVenta(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            producto.setPrecioVenta(producto.getPrecioVenta().setScale(2, RoundingMode.HALF_UP));
        }

        return productoRepository.save(producto);
    }

    private CosteoProductoResultado calcularCosteoProducto(List<ProductoInsumo> relaciones) {
        Map<Long, CosteoDetalleResultado> resultadosPorRelacion = new LinkedHashMap<>();
        List<String> advertencias = new ArrayList<>();
        BigDecimal costoMateriales = BigDecimal.ZERO;

        for (ProductoInsumo relacion : relaciones) {
            CosteoDetalleResultado detalle = calcularDetalleRelacion(relacion);
            resultadosPorRelacion.put(relacion.getId(), detalle);

            if (detalle.costoEstimado() != null) {
                costoMateriales = costoMateriales.add(detalle.costoEstimado());
            }
            if (detalle.mensajeCosto() != null) {
                advertencias.add(detalle.mensajeCosto());
            }
        }

        BigDecimal costoMaterialesEscalado = costoMateriales.setScale(4, RoundingMode.HALF_UP);
        BigDecimal costoTotal = costoMaterialesEscalado;
        BigDecimal precioSugerido = calcularPrecioSugerido(costoTotal);
        BigDecimal gananciaDirecta = precioSugerido.subtract(costoTotal).setScale(2, RoundingMode.HALF_UP);

        return new CosteoProductoResultado(
                costoMaterialesEscalado,
                costoTotal,
                precioSugerido,
                gananciaDirecta,
                advertencias.isEmpty(),
                List.copyOf(advertencias),
                resultadosPorRelacion);
    }

    private CosteoDetalleResultado calcularDetalleRelacion(ProductoInsumo productoInsumo) {
        Insumo insumo = productoInsumo.getInsumo();
        Optional<InsumoCompra> compraVigente = obtenerCompraVigente(insumo.getId());

        if (esInsumoTinta(insumo)) {
            return calcularCostoTinta(productoInsumo, compraVigente.orElse(null));
        }

        if (requiereCalculoPorArea(productoInsumo, compraVigente.orElse(null))) {
            return calcularCostoPorArea(productoInsumo, compraVigente.orElse(null));
        }

        return calcularCostoPorUnidad(productoInsumo, compraVigente.orElse(null));
    }

    private CosteoDetalleResultado calcularCostoPorArea(ProductoInsumo productoInsumo, InsumoCompra compraVigente) {
        Insumo insumo = productoInsumo.getInsumo();
        String nombreInsumo = insumo.getNombre();

        BigDecimal precioCompraTotal = obtenerPrecioCompraTotal(insumo, compraVigente);
        if (precioCompraTotal == null || precioCompraTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene precio total de compra.");
        }

        BigDecimal anchoCompra = obtenerAnchoCompra(insumo, compraVigente);
        if (anchoCompra == null || anchoCompra.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene ancho de compra.");
        }

        BigDecimal largoCompra = obtenerAltoLargoCompra(insumo, compraVigente);
        if (largoCompra == null || largoCompra.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene largo de compra.");
        }

        BigDecimal anchoUsado = productoInsumo.getAnchoUsadoCm();
        if (anchoUsado == null || anchoUsado.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene ancho usado.");
        }

        BigDecimal altoUsado = productoInsumo.getAltoLargoUsadoCm();
        if (altoUsado == null || altoUsado.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene medida usada.");
        }

        BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());
        if (cantidadUsada.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, "La cantidad usada de " + nombreInsumo + " debe ser mayor a cero.");
        }

        BigDecimal anchoCompraCm = normalizarDimensionCompraACm(insumo, compraVigente, anchoCompra);
        BigDecimal largoCompraCm = normalizarDimensionCompraACm(insumo, compraVigente, largoCompra);
        BigDecimal areaTotalCompradaCm2 = anchoCompraCm.multiply(largoCompraCm);
        if (areaTotalCompradaCm2.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene un area total de compra valida.");
        }

        BigDecimal costoPorCm2 = precioCompraTotal.divide(areaTotalCompradaCm2, 12, RoundingMode.HALF_UP);
        BigDecimal areaUsadaCm2 = anchoUsado.multiply(altoUsado).multiply(cantidadUsada);
        BigDecimal costoUsado = costoPorCm2.multiply(areaUsadaCm2).setScale(4, RoundingMode.HALF_UP);
        return new CosteoDetalleResultado(costoUsado, null);
    }

    private CosteoDetalleResultado calcularCostoPorUnidad(ProductoInsumo productoInsumo, InsumoCompra compraVigente) {
        Insumo insumo = productoInsumo.getInsumo();
        String nombreInsumo = insumo.getNombre();

        BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());
        if (cantidadUsada.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, "La cantidad usada de " + nombreInsumo + " debe ser mayor a cero.");
        }

        BigDecimal precioCompraTotal = obtenerPrecioCompraTotal(insumo, compraVigente);
        if (precioCompraTotal == null || precioCompraTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene precio total de compra.");
        }

        BigDecimal cantidadComprada = obtenerCantidadComprada(insumo, compraVigente);
        if (cantidadComprada == null || cantidadComprada.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene cantidad comprada.");
        }

        BigDecimal precioUnitario = obtenerPrecioUnitario(insumo, compraVigente, precioCompraTotal, cantidadComprada);
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene costo unitario calculable.");
        }

        BigDecimal costoUsado = precioUnitario.multiply(cantidadUsada).setScale(4, RoundingMode.HALF_UP);
        return new CosteoDetalleResultado(costoUsado, null);
    }

    private CosteoDetalleResultado calcularCostoTinta(ProductoInsumo productoInsumo, InsumoCompra compraVigente) {
        Insumo insumo = productoInsumo.getInsumo();
        String nombreInsumo = insumo.getNombre();

        BigDecimal precioCompraTotal = obtenerPrecioCompraTotal(insumo, compraVigente);
        if (precioCompraTotal == null || precioCompraTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene precio total de compra.");
        }

        BigDecimal cantidadMlComprada = obtenerCantidadComprada(insumo, compraVigente);
        if (cantidadMlComprada == null || cantidadMlComprada.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene cantidad ML comprada.");
        }

        BigDecimal mlUsado = parsearConsumoMl(productoInsumo.getConsumo());
        if (mlUsado == null) {
            BigDecimal anchoUsado = productoInsumo.getAnchoUsadoCm();
            BigDecimal altoUsado = productoInsumo.getAltoLargoUsadoCm();
            BigDecimal cantidadUsada = valorOZero(productoInsumo.getCantidadUsada());

            if (anchoUsado == null || anchoUsado.compareTo(BigDecimal.ZERO) <= 0
                    || altoUsado == null || altoUsado.compareTo(BigDecimal.ZERO) <= 0) {
                return new CosteoDetalleResultado(null, nombreInsumo + " no tiene medida usada.");
            }
            if (cantidadUsada.compareTo(BigDecimal.ZERO) <= 0) {
                return new CosteoDetalleResultado(null, "La cantidad usada de " + nombreInsumo + " debe ser mayor a cero.");
            }

            BigDecimal areaUsadaCm2 = anchoUsado.multiply(altoUsado).multiply(cantidadUsada);
            mlUsado = areaUsadaCm2.divide(AREA_BASE_TINTA_CM2, 8, RoundingMode.HALF_UP);
        }

        if (mlUsado.compareTo(BigDecimal.ZERO) <= 0) {
            return new CosteoDetalleResultado(null, nombreInsumo + " no tiene consumo ML calculable.");
        }

        BigDecimal costoPorMl = precioCompraTotal.divide(cantidadMlComprada, 8, RoundingMode.HALF_UP);
        BigDecimal costoUsado = costoPorMl.multiply(mlUsado).setScale(4, RoundingMode.HALF_UP);
        return new CosteoDetalleResultado(costoUsado, null);
    }

    private boolean requiereCalculoPorArea(ProductoInsumo productoInsumo, InsumoCompra compraVigente) {
        boolean tieneMedidasUsadas = productoInsumo.getAnchoUsadoCm() != null || productoInsumo.getAltoLargoUsadoCm() != null;
        boolean tieneMedidasCompra = obtenerAnchoCompra(productoInsumo.getInsumo(), compraVigente) != null
                || obtenerAltoLargoCompra(productoInsumo.getInsumo(), compraVigente) != null;
        return (tieneMedidasUsadas && tieneMedidasCompra) || (tieneMedidasUsadas && esInsumoPorArea(productoInsumo.getInsumo(), compraVigente));
    }

    private boolean esInsumoPorArea(Insumo insumo, InsumoCompra compraVigente) {
        String unidad = normalizeComparableText(obtenerUnidadMedida(insumo, compraVigente));
        String categoria = normalizeComparableText(insumo.getCategoria());
        String nombre = normalizeComparableText(insumo.getNombre());

        return unidad.contains("rollo")
                || unidad.contains("metro")
                || unidad.equals("m")
                || unidad.contains("metro lineal")
                || categoria.contains("textil")
                || categoria.contains("materiales textiles")
                || categoria.contains("papel")
                || nombre.contains("tela")
                || nombre.contains("bistrech")
                || nombre.contains("bistretch")
                || nombre.contains("papel");
    }

    private boolean esInsumoTinta(Insumo insumo) {
        String unidad = normalizeComparableText(insumo.getUnidadMedida());
        String categoria = normalizeComparableText(insumo.getCategoria());
        String nombre = normalizeComparableText(insumo.getNombre());

        return unidad.equals("ml")
                || unidad.contains("mililitro")
                || categoria.contains("tinta")
                || nombre.contains("tinta");
    }

    private BigDecimal normalizarDimensionCompraACm(Insumo insumo, InsumoCompra compraVigente, BigDecimal valor) {
        if (valor == null) {
            return null;
        }

        if (compraExpresadaEnMetros(insumo, compraVigente)) {
            return valor.multiply(FACTOR_METROS_A_CENTIMETROS);
        }

        return valor;
    }

    private boolean compraExpresadaEnMetros(Insumo insumo, InsumoCompra compraVigente) {
        String unidad = normalizeComparableText(obtenerUnidadMedida(insumo, compraVigente));
        String categoria = normalizeComparableText(insumo.getCategoria());
        String nombre = normalizeComparableText(insumo.getNombre());

        return unidad.equals("rollo")
                || unidad.contains("metro")
                || unidad.equals("m")
                || unidad.contains("metro lineal")
                || categoria.contains("textil")
                || categoria.contains("materiales textiles")
                || nombre.contains("tela")
                || nombre.contains("bistrech")
                || nombre.contains("bistretch")
                || nombre.contains("papel");
    }

    private BigDecimal obtenerPrecioCompraTotal(Insumo insumo, InsumoCompra compraVigente) {
        if (compraVigente != null && compraVigente.getPrecioCompraTotal() != null) {
            return compraVigente.getPrecioCompraTotal();
        }
        return insumo.getPrecioCompraTotal();
    }

    private BigDecimal obtenerCantidadComprada(Insumo insumo, InsumoCompra compraVigente) {
        if (compraVigente != null && compraVigente.getCantidadComprada() != null) {
            return compraVigente.getCantidadComprada();
        }
        return insumo.getCantidadComprada();
    }

    private BigDecimal obtenerPrecioUnitario(
            Insumo insumo,
            InsumoCompra compraVigente,
            BigDecimal precioCompraTotal,
            BigDecimal cantidadComprada) {
        if (compraVigente != null && compraVigente.getPrecioUnitario() != null
                && compraVigente.getPrecioUnitario().compareTo(BigDecimal.ZERO) > 0) {
            return compraVigente.getPrecioUnitario();
        }

        if (insumo.getCostoUnitario() != null && insumo.getCostoUnitario().compareTo(BigDecimal.ZERO) > 0) {
            return insumo.getCostoUnitario();
        }

        if (precioCompraTotal == null || cantidadComprada == null || cantidadComprada.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return precioCompraTotal.divide(cantidadComprada, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal obtenerAnchoCompra(Insumo insumo, InsumoCompra compraVigente) {
        if (compraVigente != null && compraVigente.getAncho() != null) {
            return compraVigente.getAncho();
        }
        return insumo.getAncho();
    }

    private BigDecimal obtenerAltoLargoCompra(Insumo insumo, InsumoCompra compraVigente) {
        if (compraVigente != null && compraVigente.getAlto() != null) {
            return compraVigente.getAlto();
        }
        return insumo.getAlto();
    }

    private String obtenerUnidadMedida(Insumo insumo, InsumoCompra compraVigente) {
        if (compraVigente != null && compraVigente.getUnidadMedida() != null && !compraVigente.getUnidadMedida().isBlank()) {
            return compraVigente.getUnidadMedida();
        }
        return insumo.getUnidadMedida();
    }

    private BigDecimal parsearConsumoMl(String consumo) {
        String normalized = normalizeOptionalText(consumo);
        if (normalized == null) {
            return null;
        }

        Matcher matcher = DECIMAL_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        return parseFlexibleDecimal(matcher.group(1));
    }

    private BigDecimal parseFlexibleDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal calcularPrecioSugerido(BigDecimal costoTotal) {
        if (costoTotal == null || costoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal divisor = BigDecimal.ONE.subtract(MARGEN_VENTA_DEFECTO);
        return costoTotal.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private ProductoResponse toProductoResponse(Producto producto) {
        List<ProductoInsumo> relaciones = productoInsumoRepository.findByProductoIdOrderByIdAsc(producto.getId());
        CosteoProductoResultado costeo = calcularCosteoProducto(relaciones);
        List<ProductoInsumoResponse> insumos = relaciones.stream()
                .map(relacion -> toProductoInsumoResponse(relacion, costeo.detallesPorRelacion().get(relacion.getId())))
                .toList();
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
                costeo.costoMateriales(),
                costeo.precioSugerido(),
                costeo.gananciaDirecta(),
                ultimoCambioPrecio,
                !cambiosPrecio.isEmpty(),
                costeo.costeoCompleto(),
                costeo.advertenciasCosteo(),
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

    private ProductoInsumoResponse toProductoInsumoResponse(
            ProductoInsumo productoInsumo,
            CosteoDetalleResultado detalleCosto) {
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
                detalleCosto == null ? null : detalleCosto.costoEstimado(),
                detalleCosto == null ? null : detalleCosto.mensajeCosto(),
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
                .multiply(CIEN)
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

    private String normalizeComparableText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase().replace('_', ' ');
    }

    private BigDecimal valorOZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record CosteoDetalleResultado(BigDecimal costoEstimado, String mensajeCosto) {}

    private record CosteoProductoResultado(
            BigDecimal costoMateriales,
            BigDecimal costoTotal,
            BigDecimal precioSugerido,
            BigDecimal gananciaDirecta,
            boolean costeoCompleto,
            List<String> advertenciasCosteo,
            Map<Long, CosteoDetalleResultado> detallesPorRelacion) {}
}
