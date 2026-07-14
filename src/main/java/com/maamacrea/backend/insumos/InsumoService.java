package com.maamacrea.backend.insumos;

import com.maamacrea.backend.ApiRequestException;
import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.productos.ProductoInsumoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class InsumoService {
    private static final BigDecimal FACTOR_YARDA_A_METROS = new BigDecimal("0.9144");

    private final InsumoRepository insumoRepository;
    private final InsumoCompraRepository insumoCompraRepository;
    private final ProductoInsumoRepository productoInsumoRepository;
    private final InsumoDocumentoStorageService insumoDocumentoStorageService;

    public InsumoService(
            InsumoRepository insumoRepository,
            InsumoCompraRepository insumoCompraRepository,
            ProductoInsumoRepository productoInsumoRepository,
            InsumoDocumentoStorageService insumoDocumentoStorageService) {
        this.insumoRepository = insumoRepository;
        this.insumoCompraRepository = insumoCompraRepository;
        this.productoInsumoRepository = productoInsumoRepository;
        this.insumoDocumentoStorageService = insumoDocumentoStorageService;
    }

    public List<InsumoResponse> listarTodos() {
        return insumoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InsumoResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    public List<InsumoCompraResponse> listarCompras(Long insumoId) {
        obtenerEntidad(insumoId);
        return obtenerComprasOrdenadas(insumoId).stream().map(this::toCompraResponse).toList();
    }

    public InsumoCompraResponse obtenerPrecioVigente(Long insumoId) {
        obtenerEntidad(insumoId);
        InsumoCompra compra = obtenerCompraVigenteInterna(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe historial de compras para este insumo."));
        return toCompraResponse(compra);
    }

    @Transactional
    public InsumoResponse crear(InsumoRequest insumoRequest) {
        Insumo insumo = new Insumo();
        aplicarCamposBase(insumo, insumoRequest);
        CompraInput compraInput = buildCompraInput(insumoRequest);
        sincronizarResumenCompraInicial(insumo, compraInput);
        Insumo guardado = insumoRepository.save(insumo);
        registrarNuevaCompraInterna(guardado, compraInput);
        return toResponse(insumoRepository.save(guardado));
    }

    @Transactional
    public InsumoResponse actualizar(Long id, InsumoRequest insumoRequest) {
        Insumo insumo = obtenerEntidad(id);
        aplicarCamposBase(insumo, insumoRequest);

        CompraInput compraInput = buildCompraInput(insumoRequest);
        InsumoCompra compraVigente = obtenerCompraVigenteInterna(id).orElse(null);

        if (compraVigente == null) {
            registrarNuevaCompraInterna(insumo, compraInput);
        } else if (tieneCambioDeCompra(compraVigente, compraInput)) {
            registrarNuevaCompraInterna(insumo, compraInput);
        } else {
            actualizarMetadatosCompra(compraVigente, compraInput);
            InsumoCompra compraActualizada = insumoCompraRepository.save(compraVigente);
            sincronizarResumenCompra(insumo, compraActualizada);
        }

        return toResponse(insumoRepository.save(insumo));
    }

    @Transactional
    public InsumoCompraResponse registrarCompra(Long id, InsumoCompraRequest compraRequest) {
        Insumo insumo = obtenerEntidad(id);
        InsumoCompra compra = registrarNuevaCompraInterna(insumo, buildCompraInput(compraRequest));
        insumoRepository.save(insumo);
        return toCompraResponse(compra);
    }

    @Transactional
    public InsumoResponse actualizarDocumento(Long id, MultipartFile file) {
        Insumo insumo = obtenerEntidad(id);
        String documentoUrl = insumoDocumentoStorageService.guardarDocumento(id, file);
        insumo.setDocumentoUrl(documentoUrl);

        obtenerCompraVigenteInterna(id).ifPresent(compra -> {
            compra.setDocumentoUrl(documentoUrl);
            insumoCompraRepository.save(compra);
        });

        return toResponse(insumoRepository.save(insumo));
    }

    @Transactional
    public void eliminar(Long id) {
        Insumo insumo = obtenerEntidad(id);
        if (productoInsumoRepository.existsByInsumoId(id)) {
            throw buildInsumoConDependenciasException();
        }

        try {
            insumoRepository.delete(insumo);
            insumoRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw buildInsumoConDependenciasException();
        }
    }

    private void aplicarCamposBase(Insumo insumo, InsumoRequest insumoRequest) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumoRequest.categoria());
        String codigoProducto = normalizarCodigoProducto(insumoRequest.codigoProducto());

        validarCodigoProductoDisponible(codigoProducto, insumo.getId());

        insumo.setCodigoProducto(codigoProducto);
        insumo.setNombre(insumoRequest.nombre().trim());
        insumo.setCategoria(categoria.getCodigo());
        insumo.setProveedor(normalizarTexto(insumoRequest.proveedor()));
    }

    private InsumoCompra registrarNuevaCompraInterna(Insumo insumo, CompraInput compraInput) {
        validarCompraInput(compraInput);
        insumoCompraRepository.clearVigenteByInsumoId(insumo.getId());

        InsumoCompra compra = new InsumoCompra();
        compra.setInsumo(insumo);
        aplicarCompraInput(compra, compraInput);
        compra.setVigente(true);

        InsumoCompra guardada = insumoCompraRepository.save(compra);
        sincronizarResumenCompra(insumo, guardada);
        return guardada;
    }

    private void aplicarCompraInput(InsumoCompra compra, CompraInput compraInput) {
        BigDecimal contenidoTotalComprado = calcularContenidoTotalComprado(
                compraInput.cantidadComprada(),
                compraInput.contenidoPorUnidad(),
                compraInput.unidadContenido(),
                compraInput.unidadMedida());
        BigDecimal precioUnitario = calcularPrecioUnitario(compraInput, contenidoTotalComprado);
        compra.setFechaCompra(compraInput.fechaCompra());
        compra.setCantidadComprada(compraInput.cantidadComprada().setScale(3, RoundingMode.HALF_UP));
        compra.setCantidadMlComprados(scaleOptional(compraInput.cantidadMlComprados(), 4));
        compra.setContenidoPorUnidad(scaleOptional(compraInput.contenidoPorUnidad(), 4));
        compra.setUnidadContenido(compraInput.unidadContenido());
        compra.setContenidoTotalComprado(scaleOptional(contenidoTotalComprado, 4));
        compra.setUnidadMedida(compraInput.unidadMedida());
        compra.setPrecioCompraTotal(compraInput.precioCompraTotal().setScale(2, RoundingMode.HALF_UP));
        compra.setPrecioUnitario(precioUnitario);
        compra.setPrecioNeto(scaleOptional(compraInput.precioNeto(), 2));
        compra.setIva(scaleOptional(compraInput.iva(), 2));
        compra.setAncho(scaleOptional(compraInput.ancho(), 3));
        compra.setUnidadAncho(compraInput.unidadAncho());
        compra.setAlto(scaleOptional(compraInput.alto(), 3));
        compra.setUnidadAlto(compraInput.unidadAlto());
        compra.setTipoDocumento(compraInput.tipoDocumento());
        compra.setNumeroDocumento(compraInput.numeroDocumento());
        compra.setDocumentoUrl(compraInput.documentoUrl());
        compra.setObservacion(compraInput.observacion());
    }

    private void actualizarMetadatosCompra(InsumoCompra compra, CompraInput compraInput) {
        compra.setFechaCompra(compraInput.fechaCompra());
        compra.setTipoDocumento(compraInput.tipoDocumento());
        compra.setNumeroDocumento(compraInput.numeroDocumento());
        compra.setDocumentoUrl(compraInput.documentoUrl());
        compra.setObservacion(compraInput.observacion());
        compra.setVigente(true);
    }

    private void sincronizarResumenCompra(Insumo insumo, InsumoCompra compra) {
        insumo.setUnidadMedida(compra.getUnidadMedida());
        insumo.setCantidadComprada(compra.getCantidadComprada());
        insumo.setCantidadMlComprados(compra.getCantidadMlComprados());
        insumo.setContenidoPorUnidad(compra.getContenidoPorUnidad());
        insumo.setUnidadContenido(compra.getUnidadContenido());
        insumo.setContenidoTotalComprado(compra.getContenidoTotalComprado());
        insumo.setAncho(compra.getAncho());
        insumo.setUnidadAncho(compra.getUnidadAncho());
        insumo.setAlto(compra.getAlto());
        insumo.setUnidadAlto(compra.getUnidadAlto());
        insumo.setPrecioNeto(compra.getPrecioNeto());
        insumo.setIva(compra.getIva());
        insumo.setPrecioCompraTotal(compra.getPrecioCompraTotal());
        insumo.setCostoUnitario(compra.getPrecioUnitario());
        insumo.setFechaCompra(compra.getFechaCompra());
        insumo.setTipoDocumento(compra.getTipoDocumento());
        insumo.setNumeroDocumento(compra.getNumeroDocumento());
        insumo.setDocumentoUrl(compra.getDocumentoUrl());
        insumo.setNotas(compra.getObservacion());
    }

    private void sincronizarResumenCompraInicial(Insumo insumo, CompraInput compraInput) {
        validarCompraInput(compraInput);

        InsumoCompra compraTemporal = new InsumoCompra();
        aplicarCompraInput(compraTemporal, compraInput);
        sincronizarResumenCompra(insumo, compraTemporal);
    }

    private CompraInput buildCompraInput(InsumoRequest insumoRequest) {
        String unidadMedida = normalizarUnidadMedida(insumoRequest.unidadMedida());
        boolean esCono = isUnidadCono(unidadMedida);
        return new CompraInput(
                insumoRequest.fechaCompra(),
                insumoRequest.cantidadComprada(),
                insumoRequest.cantidadMlComprados(),
                esCono ? insumoRequest.contenidoPorUnidad() : null,
                esCono ? normalizarUnidadContenido(insumoRequest.unidadContenido()) : null,
                unidadMedida,
                resolverPrecioCompraTotal(insumoRequest.precioCompraTotal(), insumoRequest.precioNeto(), insumoRequest.iva()),
                insumoRequest.precioNeto(),
                insumoRequest.iva(),
                insumoRequest.ancho(),
                normalizarUnidadDimension(insumoRequest.unidadAncho()),
                insumoRequest.alto(),
                normalizarUnidadDimension(insumoRequest.unidadAlto()),
                normalizarTipoDocumento(insumoRequest.tipoDocumento()),
                normalizarTexto(insumoRequest.numeroDocumento()),
                normalizarTexto(insumoRequest.documentoUrl()),
                normalizarTexto(insumoRequest.notas()));
    }

    private CompraInput buildCompraInput(InsumoCompraRequest compraRequest) {
        String unidadMedida = normalizarUnidadMedida(compraRequest.unidadMedida());
        boolean esCono = isUnidadCono(unidadMedida);
        return new CompraInput(
                compraRequest.fechaCompra(),
                compraRequest.cantidadComprada(),
                compraRequest.cantidadMlComprados(),
                esCono ? compraRequest.contenidoPorUnidad() : null,
                esCono ? normalizarUnidadContenido(compraRequest.unidadContenido()) : null,
                unidadMedida,
                compraRequest.precioCompraTotal(),
                compraRequest.precioNeto(),
                compraRequest.iva(),
                compraRequest.ancho(),
                normalizarUnidadDimension(compraRequest.unidadAncho()),
                compraRequest.alto(),
                normalizarUnidadDimension(compraRequest.unidadAlto()),
                normalizarTipoDocumento(compraRequest.tipoDocumento()),
                normalizarTexto(compraRequest.numeroDocumento()),
                normalizarTexto(compraRequest.documentoUrl()),
                normalizarTexto(compraRequest.observacion()));
    }

    private void validarCompraInput(CompraInput compraInput) {
        if (compraInput.fechaCompra() == null) {
            throw new IllegalArgumentException("La fecha de compra es obligatoria.");
        }
        if (compraInput.cantidadComprada() == null || compraInput.cantidadComprada().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad comprada debe ser mayor a cero.");
        }
        if (compraInput.unidadMedida() == null || compraInput.unidadMedida().isBlank()) {
            throw new IllegalArgumentException("La unidad de medida es obligatoria.");
        }
        if (isUnidadMililitros(compraInput.unidadMedida())
                && (compraInput.cantidadMlComprados() == null
                        || compraInput.cantidadMlComprados().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("La cantidad de ML comprados es obligatoria para insumos medidos en ML.");
        }
        if (isUnidadCono(compraInput.unidadMedida())) {
            if (compraInput.contenidoPorUnidad() == null || compraInput.contenidoPorUnidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El contenido por cono es obligatorio para insumos medidos como cono.");
            }
            if (compraInput.unidadContenido() == null || compraInput.unidadContenido().isBlank()) {
                throw new IllegalArgumentException("La unidad de contenido es obligatoria para insumos medidos como cono.");
            }
        }
        if (compraInput.precioCompraTotal() == null || compraInput.precioCompraTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio total de compra debe ser mayor a cero.");
        }
        validarMedidaPositiva(compraInput.ancho(), "El ancho debe ser mayor a cero.");
        validarMedidaPositiva(compraInput.alto(), "El alto debe ser mayor a cero.");
        if (compraInput.ancho() != null && (compraInput.unidadAncho() == null || compraInput.unidadAncho().isBlank())) {
            throw new IllegalArgumentException("La unidad del ancho es obligatoria cuando se informa un ancho de compra.");
        }
        if (compraInput.alto() != null && (compraInput.unidadAlto() == null || compraInput.unidadAlto().isBlank())) {
            throw new IllegalArgumentException("La unidad del alto o largo es obligatoria cuando se informa una medida de largo de compra.");
        }
        validarNoNegativo(compraInput.precioNeto(), "El precio neto no puede ser negativo.");
        validarNoNegativo(compraInput.iva(), "El IVA no puede ser negativo.");
    }

    private void validarCodigoProductoDisponible(String codigoProducto, Long insumoId) {
        boolean codigoDuplicado =
                insumoId == null
                        ? insumoRepository.existsByCodigoProductoIgnoreCase(codigoProducto)
                        : insumoRepository.existsByCodigoProductoIgnoreCaseAndIdNot(codigoProducto, insumoId);

        if (codigoDuplicado) {
            throw new IllegalArgumentException(
                    "Ya existe un insumo con el codigo de producto informado.");
        }
    }

    private Optional<InsumoCompra> obtenerCompraVigenteInterna(Long insumoId) {
        Optional<InsumoCompra> vigente =
                insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        if (vigente != null && vigente.isPresent()) {
            return vigente;
        }

        Optional<InsumoCompra> ultima =
                insumoCompraRepository.findFirstByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        return ultima == null ? Optional.empty() : ultima;
    }

    private List<InsumoCompra> obtenerComprasOrdenadas(Long insumoId) {
        List<InsumoCompra> compras =
                insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        return compras == null ? List.of() : compras;
    }

    private CompraResumen construirResumenCompra(Long insumoId) {
        List<InsumoCompra> compras = obtenerComprasOrdenadas(insumoId);
        if (compras.isEmpty()) {
            return new CompraResumen(null, 0, false, null, null, null);
        }

        InsumoCompra vigente = obtenerCompraVigenteInterna(insumoId).orElse(compras.get(0));
        InsumoCompra anterior = compras.stream()
                .filter(compra -> !Objects.equals(compra.getId(), vigente.getId()))
                .findFirst()
                .orElse(null);

        boolean tieneCambioPrecio = anterior != null
                && !sameBigDecimal(vigente.getPrecioCompraTotal(), anterior.getPrecioCompraTotal());
        return new CompraResumen(
                vigente.getId(),
                compras.size(),
                tieneCambioPrecio,
                tieneCambioPrecio ? vigente.getFechaCompra() : null,
                anterior == null ? null : anterior.getPrecioCompraTotal(),
                calcularVariacionPorcentual(
                        anterior == null ? null : anterior.getPrecioCompraTotal(),
                        vigente.getPrecioCompraTotal()));
    }

    private boolean tieneCambioDeCompra(InsumoCompra compraActual, CompraInput compraInput) {
        return !sameBigDecimal(compraActual.getCantidadComprada(), compraInput.cantidadComprada())
                || !sameBigDecimal(compraActual.getCantidadMlComprados(), compraInput.cantidadMlComprados())
                || !sameBigDecimal(compraActual.getContenidoPorUnidad(), compraInput.contenidoPorUnidad())
                || !Objects.equals(normalizarUnidadContenido(compraActual.getUnidadContenido()), compraInput.unidadContenido())
                || !sameBigDecimal(
                        compraActual.getContenidoTotalComprado(),
                        calcularContenidoTotalComprado(
                                compraInput.cantidadComprada(),
                                compraInput.contenidoPorUnidad(),
                                compraInput.unidadContenido(),
                                compraInput.unidadMedida()))
                || !Objects.equals(normalizarUnidadMedida(compraActual.getUnidadMedida()), compraInput.unidadMedida())
                || !sameBigDecimal(compraActual.getPrecioCompraTotal(), compraInput.precioCompraTotal())
                || !sameBigDecimal(compraActual.getPrecioNeto(), compraInput.precioNeto())
                || !sameBigDecimal(compraActual.getIva(), compraInput.iva())
                || !sameBigDecimal(compraActual.getAncho(), compraInput.ancho())
                || !Objects.equals(normalizarUnidadDimension(compraActual.getUnidadAncho()), compraInput.unidadAncho())
                || !sameBigDecimal(compraActual.getAlto(), compraInput.alto())
                || !Objects.equals(normalizarUnidadDimension(compraActual.getUnidadAlto()), compraInput.unidadAlto());
    }

    private BigDecimal resolverPrecioCompraTotal(
            BigDecimal precioCompraTotal, BigDecimal precioNeto, BigDecimal iva) {
        if (precioCompraTotal != null) {
            return precioCompraTotal;
        }

        if (precioNeto != null && iva != null) {
            return precioNeto.add(iva);
        }

        return null;
    }

    private BigDecimal calcularPrecioUnitario(CompraInput compraInput, BigDecimal contenidoTotalComprado) {
        BigDecimal divisor;
        if (isUnidadMililitros(compraInput.unidadMedida())) {
            divisor = compraInput.cantidadMlComprados();
        } else if (isUnidadCono(compraInput.unidadMedida())) {
            divisor = contenidoTotalComprado;
        } else {
            divisor = compraInput.cantidadComprada();
        }

        return compraInput.precioCompraTotal().divide(divisor, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularContenidoTotalComprado(
            BigDecimal cantidadComprada,
            BigDecimal contenidoPorUnidad,
            String unidadContenido,
            String unidadMedida) {
        if (!isUnidadCono(unidadMedida) || cantidadComprada == null || contenidoPorUnidad == null) {
            return null;
        }

        BigDecimal contenidoNormalizado = convertirContenidoAMetros(contenidoPorUnidad, unidadContenido);
        if (contenidoNormalizado == null) {
            return null;
        }

        return cantidadComprada.multiply(contenidoNormalizado);
    }

    private BigDecimal calcularVariacionPorcentual(BigDecimal precioAnterior, BigDecimal precioActual) {
        if (precioAnterior == null || precioActual == null || precioAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return precioActual.subtract(precioAnterior)
                .multiply(new BigDecimal("100"))
                .divide(precioAnterior, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOptional(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
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

    private void validarNoNegativo(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private Insumo obtenerEntidad(Long id) {
        return insumoRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));
    }

    private ApiRequestException buildInsumoConDependenciasException() {
        return new ApiRequestException(
                HttpStatus.CONFLICT,
                "INSUMO_CON_DEPENDENCIAS",
                "No se puede eliminar este insumo porque tiene movimientos o registros asociados. Puedes desactivarlo en lugar de eliminarlo.");
    }

    private String normalizarCodigoProducto(String codigoProducto) {
        return codigoProducto.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarUnidadMedida(String unidadMedida) {
        return unidadMedida == null ? null : unidadMedida.trim();
    }

    private boolean isUnidadMililitros(String unidadMedida) {
        String valorNormalizado = normalizarUnidadMedida(unidadMedida);
        if (valorNormalizado == null) {
            return false;
        }

        String comparable = valorNormalizado.toLowerCase(Locale.ROOT);
        return comparable.equals("ml") || comparable.equals("mililitro") || comparable.equals("mililitros");
    }

    private boolean isUnidadCono(String unidadMedida) {
        String valorNormalizado = normalizarUnidadMedida(unidadMedida);
        if (valorNormalizado == null) {
            return false;
        }

        return valorNormalizado.toLowerCase(Locale.ROOT).equals("cono");
    }

    private String normalizarTipoDocumento(String tipoDocumento) {
        String valorNormalizado = normalizarTexto(tipoDocumento);
        if (valorNormalizado == null) {
            return null;
        }

        return InsumoTipoDocumento.fromCodigo(valorNormalizado.toUpperCase(Locale.ROOT)).getCodigo();
    }

    private String normalizarUnidadDimension(String unidadDimension) {
        String valorNormalizado = normalizarTexto(unidadDimension);
        return valorNormalizado == null ? null : valorNormalizado.toLowerCase(Locale.ROOT);
    }

    private String normalizarUnidadContenido(String unidadContenido) {
        String valorNormalizado = normalizarTexto(unidadContenido);
        return valorNormalizado == null ? null : valorNormalizado.toLowerCase(Locale.ROOT);
    }

    private BigDecimal convertirContenidoAMetros(BigDecimal valor, String unidadContenido) {
        if (valor == null) {
            return null;
        }

        String unidad = normalizarUnidadContenido(unidadContenido);
        if (unidad == null) {
            return null;
        }

        if (unidad.equals("m") || unidad.equals("metro") || unidad.equals("metros")) {
            return valor;
        }

        if (unidad.equals("yd") || unidad.equals("yarda") || unidad.equals("yardas")) {
            return valor.multiply(FACTOR_YARDA_A_METROS);
        }

        return null;
    }

    private InsumoResponse toResponse(Insumo insumo) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumo.getCategoria());
        CompraResumen resumen = construirResumenCompra(insumo.getId());

        return new InsumoResponse(
                insumo.getId(),
                insumo.getCodigoProducto(),
                insumo.getNombre(),
                categoria.getCodigo(),
                categoria.getLabel(),
                insumo.getUnidadMedida(),
                insumo.getCantidadComprada(),
                insumo.getCantidadMlComprados(),
                insumo.getContenidoPorUnidad(),
                insumo.getUnidadContenido(),
                insumo.getContenidoTotalComprado(),
                insumo.getAncho(),
                insumo.getUnidadAncho(),
                insumo.getAlto(),
                insumo.getUnidadAlto(),
                insumo.getPrecioNeto(),
                insumo.getIva(),
                insumo.getPrecioCompraTotal(),
                insumo.getCostoUnitario(),
                insumo.getProveedor(),
                insumo.getFechaCompra(),
                insumo.getTipoDocumento(),
                insumo.getNumeroDocumento(),
                insumo.getDocumentoUrl(),
                insumo.getNotas(),
                resumen.compraVigenteId(),
                resumen.totalCompras(),
                resumen.tieneCambioPrecio(),
                resumen.ultimoCambioPrecio(),
                resumen.precioCompraAnterior(),
                resumen.variacionPrecioPorcentual(),
                insumo.getCreatedAt(),
                insumo.getUpdatedAt());
    }

    private InsumoCompraResponse toCompraResponse(InsumoCompra compra) {
        return new InsumoCompraResponse(
                compra.getId(),
                compra.getInsumo().getId(),
                compra.getFechaCompra(),
                compra.getCantidadComprada(),
                compra.getCantidadMlComprados(),
                compra.getContenidoPorUnidad(),
                compra.getUnidadContenido(),
                compra.getContenidoTotalComprado(),
                compra.getUnidadMedida(),
                compra.getPrecioCompraTotal(),
                compra.getPrecioUnitario(),
                compra.getPrecioNeto(),
                compra.getIva(),
                compra.getAncho(),
                compra.getUnidadAncho(),
                compra.getAlto(),
                compra.getUnidadAlto(),
                compra.getTipoDocumento(),
                compra.getNumeroDocumento(),
                compra.getDocumentoUrl(),
                compra.getObservacion(),
                compra.isVigente(),
                compra.getCreatedAt(),
                compra.getUpdatedAt());
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }

    private record CompraInput(
            LocalDate fechaCompra,
            BigDecimal cantidadComprada,
            BigDecimal cantidadMlComprados,
            BigDecimal contenidoPorUnidad,
            String unidadContenido,
            String unidadMedida,
            BigDecimal precioCompraTotal,
            BigDecimal precioNeto,
            BigDecimal iva,
            BigDecimal ancho,
            String unidadAncho,
            BigDecimal alto,
            String unidadAlto,
            String tipoDocumento,
            String numeroDocumento,
            String documentoUrl,
            String observacion) {}

    private record CompraResumen(
            Long compraVigenteId,
            long totalCompras,
            boolean tieneCambioPrecio,
            LocalDate ultimoCambioPrecio,
            BigDecimal precioCompraAnterior,
            BigDecimal variacionPrecioPorcentual) {}
}
