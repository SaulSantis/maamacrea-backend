package com.maamacrea.backend.ventas;

import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.productos.ProductoResponse;
import com.maamacrea.backend.productos.ProductoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class VentaService {

    private static final Set<VentaEstadoPedido> ESTADOS_ACTIVOS = Set.of(
            VentaEstadoPedido.PAGO_CONFIRMADO,
            VentaEstadoPedido.DISENO_Y_CONFECCION,
            VentaEstadoPedido.ENTREGADO_A_CORREOS,
            VentaEstadoPedido.RECIBIDO_POR_CLIENTE,
            VentaEstadoPedido.VENTA_FINALIZADA);

    private final VentaRepository ventaRepository;
    private final VentaArchivoDisenoRepository ventaArchivoDisenoRepository;
    private final ProductoService productoService;
    private final VentaImagenStorageService ventaImagenStorageService;
    private final int maxArchivosPorVenta;

    public VentaService(
            VentaRepository ventaRepository,
            VentaArchivoDisenoRepository ventaArchivoDisenoRepository,
            ProductoService productoService,
            VentaImagenStorageService ventaImagenStorageService,
            @Value("${app.ventas.archivos.max-por-venta:10}")
                    int maxArchivosPorVenta) {
        this.ventaRepository = ventaRepository;
        this.ventaArchivoDisenoRepository = ventaArchivoDisenoRepository;
        this.productoService = productoService;
        this.ventaImagenStorageService = ventaImagenStorageService;
        this.maxArchivosPorVenta = Math.max(1, maxArchivosPorVenta);
    }

    public List<VentaResponse> listarTodas() {
        return ventaRepository.findAllByOrderByFechaVentaDescCreatedAtDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<VentaResponse> listarUltimas(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        return ventaRepository.findAllByOrderByFechaVentaDescCreatedAtDescIdDesc(PageRequest.of(0, safeLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    public VentaResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional
    public VentaResponse crear(VentaRequest request) {
        Venta venta = buildVentaNueva(request);
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponse crear(VentaMultipartRequest request, List<MultipartFile> archivos) {
        VentaRequest ventaRequest = requireVentaRequest(request);
        Venta venta = ventaRepository.save(buildVentaNueva(ventaRequest));
        return persistFileChanges(venta, List.of(), archivos);
    }

    @Transactional
    public VentaResponse actualizar(Long id, VentaRequest request) {
        Venta venta = applyVentaUpdate(id, request);
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponse actualizar(Long id, VentaMultipartRequest request, List<MultipartFile> archivos) {
        Venta venta = applyVentaUpdate(id, requireVentaRequest(request));
        return persistFileChanges(venta, request.normalizedDeletedFileIds(), archivos);
    }

    @Transactional
    public VentaResponse actualizarEstado(Long id, VentaEstadoUpdateRequest request) {
        if (request == null || request.estadoPedido() == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio.");
        }
        validarEstadoActivo(request.estadoPedido());

        Venta venta = obtenerEntidad(id);
        venta.setEstadoPedido(request.estadoPedido());
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponse agregarArchivos(Long id, List<MultipartFile> archivos) {
        Venta venta = obtenerEntidad(id);
        return persistFileChanges(venta, List.of(), archivos);
    }

    @Transactional
    public VentaResponse actualizarImagenDiseno(Long id, MultipartFile file) {
        return agregarArchivos(id, List.of(file));
    }

    public VentaImagenStorageService.StoredVentaDesignFile obtenerImagenDiseno(Long id) {
        Venta venta = obtenerEntidad(id);
        VentaArchivoDiseno archivoPrincipal = venta.getArchivosDiseno().stream()
                .sorted(Comparator.comparing(VentaArchivoDiseno::getOrdenVisual).thenComparing(VentaArchivoDiseno::getId))
                .findFirst()
                .orElse(null);

        if (archivoPrincipal != null) {
            return ventaImagenStorageService.cargarImagen(archivoPrincipal.getRutaAlmacenamiento());
        }

        return ventaImagenStorageService.cargarImagen(venta.getImagenDisenoUrl());
    }

    public VentaImagenStorageService.StoredVentaDesignFile obtenerArchivoDiseno(Long ventaId, Long archivoId) {
        VentaArchivoDiseno archivo = ventaArchivoDisenoRepository.findByIdAndVentaId(archivoId, ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo de venta no encontrado: " + archivoId));
        return ventaImagenStorageService.cargarImagen(archivo.getRutaAlmacenamiento());
    }

    @Transactional
    public VentaResponse eliminarArchivo(Long ventaId, Long archivoId) {
        Venta venta = obtenerEntidad(ventaId);
        return persistFileChanges(venta, List.of(archivoId), List.of());
    }

    @Transactional
    public void eliminar(Long id) {
        Venta venta = obtenerEntidad(id);
        LinkedHashSet<String> storedPaths = new LinkedHashSet<>();
        venta.getArchivosDiseno().stream()
                .map(VentaArchivoDiseno::getRutaAlmacenamiento)
                .filter(this::hasText)
                .forEach(storedPaths::add);
        if (hasText(venta.getImagenDisenoUrl())) {
            storedPaths.add(venta.getImagenDisenoUrl());
        }

        ventaRepository.delete(venta);
        registerFileSynchronization(List.of(), new ArrayList<>(storedPaths));
    }

    private VentaRequest requireVentaRequest(VentaMultipartRequest request) {
        if (request == null || request.venta() == null) {
            throw new IllegalArgumentException("Los datos de la venta son obligatorios.");
        }

        return request.venta();
    }

    private Venta buildVentaNueva(VentaRequest request) {
        validarRequest(request);

        ProductoResponse producto = productoService.buscarPorId(request.productoId());
        BigDecimal totalVenta = request.totalVenta().setScale(2, RoundingMode.HALF_UP);
        BigDecimal costoMateriales = valorOZero(producto.costoMateriales(), BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        BigDecimal costoReposicion = costoMateriales;
        BigDecimal costoTotal = costoMateriales.setScale(4, RoundingMode.HALF_UP);
        BigDecimal valorVentaSnapshot = totalVenta;
        BigDecimal precioSugerido = producto.precioSugerido() == null
                ? null
                : producto.precioSugerido().setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaDirecta = totalVenta.subtract(costoMateriales).setScale(2, RoundingMode.HALF_UP);

        Venta venta = new Venta();
        venta.setProductoId(producto.id());
        venta.setCodigoProductoBase(producto.codigo());
        venta.setNombreProductoBase(producto.nombre());
        venta.setTipoProducto(producto.tipoProducto());
        venta.setCostoMaterialesSnapshot(costoMateriales);
        venta.setCostoReposicionSnapshot(costoReposicion);
        venta.setCostoTotalSnapshot(costoTotal);
        venta.setValorVentaSnapshot(valorVentaSnapshot);
        venta.setPrecioSugeridoSnapshot(precioSugerido);
        venta.setGananciaDirectaSnapshot(gananciaDirecta);

        applyEditableFields(venta, request);
        return venta;
    }

    private Venta applyVentaUpdate(Long id, VentaRequest request) {
        validarRequest(request);

        Venta venta = obtenerEntidad(id);
        if (!venta.getProductoId().equals(request.productoId())) {
            throw new IllegalArgumentException("No es posible cambiar el producto base de una venta existente.");
        }

        applyEditableFields(venta, request);
        return venta;
    }

    private void applyEditableFields(Venta venta, VentaRequest request) {
        BigDecimal cantidad = request.cantidad().setScale(3, RoundingMode.HALF_UP);
        BigDecimal precioUnitario = request.precioUnitario().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalVenta = request.totalVenta().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoPagado = request.montoPagado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorEnvio = valorOZero(request.valorEnvio(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        venta.setCodigoVendido(normalizarTextoObligatorio(request.codigoVendido()));
        venta.setColeccionDiseno(normalizarTexto(request.coleccionDiseno()));
        venta.setReferenciaDiseno(normalizarTexto(request.referenciaDiseno()));
        venta.setCantidad(cantidad);
        venta.setPrecioUnitario(precioUnitario);
        venta.setTotalVenta(totalVenta);
        venta.setClienteNombre(normalizarTextoObligatorio(request.clienteNombre()));
        venta.setClienteApellidos(normalizarTexto(request.clienteApellidos()));
        venta.setClienteRut(normalizarTexto(request.clienteRut()));
        venta.setClienteTelefono(normalizarTextoObligatorio(request.clienteTelefono()));
        venta.setClienteEmail(normalizarTexto(request.clienteEmail()));
        venta.setClienteDireccion(normalizarTexto(request.clienteDireccion()));
        venta.setReferenciasDireccion(normalizarTexto(request.referenciasDireccion()));
        venta.setClienteComuna(normalizarTexto(request.clienteComuna()));
        venta.setValorEnvio(valorEnvio);
        venta.setMetodoPago(request.metodoPago());
        venta.setFechaPago(request.fechaPago());
        venta.setMontoPagado(montoPagado);
        venta.setEstadoPedido(request.estadoPedido());
        venta.setFechaVenta(request.fechaVenta());
    }

    private VentaResponse persistFileChanges(Venta venta, List<Long> archivosEliminarIds, List<MultipartFile> archivosNuevos) {
        List<Long> normalizedDeleteIds = normalizeDeletedFileIds(archivosEliminarIds);
        List<MultipartFile> normalizedNewFiles = normalizeUploadedFiles(archivosNuevos);

        List<VentaArchivoDiseno> archivosAEliminar = resolveArchivosToDelete(venta, normalizedDeleteIds);
        int totalArchivosFinal = venta.getArchivosDiseno().size() - archivosAEliminar.size() + normalizedNewFiles.size();
        validateTotalArchivos(totalArchivosFinal);

        List<VentaImagenStorageService.StoredVentaUpload> storedUploads =
                storeNewFiles(venta.getId(), normalizedNewFiles);
        List<String> newStoredPaths = storedUploads.stream()
                .map(VentaImagenStorageService.StoredVentaUpload::storedPath)
                .toList();
        List<String> storedPathsToDelete = archivosAEliminar.stream()
                .map(VentaArchivoDiseno::getRutaAlmacenamiento)
                .filter(this::hasText)
                .distinct()
                .toList();
        boolean fileStateChanged = !archivosAEliminar.isEmpty() || !storedUploads.isEmpty();

        try {
            for (VentaArchivoDiseno archivo : archivosAEliminar) {
                venta.removeArchivoDiseno(archivo);
            }

            for (VentaImagenStorageService.StoredVentaUpload storedUpload : storedUploads) {
                VentaArchivoDiseno archivo = new VentaArchivoDiseno();
                archivo.setNombreOriginal(storedUpload.originalFileName());
                archivo.setNombreAlmacenado(storedUpload.storedFileName());
                archivo.setRutaAlmacenamiento(storedUpload.storedPath());
                archivo.setTipoMime(storedUpload.mediaType());
                archivo.setTamanoBytes(storedUpload.sizeBytes());
                venta.addArchivoDiseno(archivo);
            }

            normalizeArchivoOrder(venta);
            syncLegacyImageUrl(venta, fileStateChanged);
            Venta savedVenta = ventaRepository.save(venta);
            registerFileSynchronization(newStoredPaths, storedPathsToDelete);
            return toResponse(savedVenta);
        } catch (RuntimeException exception) {
            cleanupStoredFilesImmediately(newStoredPaths);
            throw exception;
        }
    }

    private void validateTotalArchivos(int totalArchivosFinal) {
        if (totalArchivosFinal > maxArchivosPorVenta) {
            throw new IllegalArgumentException(
                    "Solo puedes guardar hasta " + maxArchivosPorVenta + " archivos por venta.");
        }
    }

    private List<Long> normalizeDeletedFileIds(List<Long> archivosEliminarIds) {
        if (archivosEliminarIds == null) {
            return List.of();
        }

        return archivosEliminarIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<MultipartFile> normalizeUploadedFiles(List<MultipartFile> archivos) {
        if (archivos == null) {
            return List.of();
        }

        List<MultipartFile> normalizedFiles = archivos.stream()
                .filter(java.util.Objects::nonNull)
                .toList();

        if (normalizedFiles.stream().anyMatch(MultipartFile::isEmpty)) {
            throw new IllegalArgumentException("El archivo seleccionado esta vacio.");
        }

        return normalizedFiles;
    }

    private List<VentaArchivoDiseno> resolveArchivosToDelete(Venta venta, List<Long> archivosEliminarIds) {
        if (archivosEliminarIds.isEmpty()) {
            return List.of();
        }

        return archivosEliminarIds.stream()
                .map(id -> venta.getArchivosDiseno().stream()
                        .filter(archivo -> id.equals(archivo.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Archivo de venta no encontrado: " + id)))
                .toList();
    }

    private List<VentaImagenStorageService.StoredVentaUpload> storeNewFiles(Long ventaId, List<MultipartFile> archivosNuevos) {
        if (archivosNuevos.isEmpty()) {
            return List.of();
        }

        List<VentaImagenStorageService.StoredVentaUpload> storedUploads = new ArrayList<>();
        try {
            for (MultipartFile archivo : archivosNuevos) {
                storedUploads.add(ventaImagenStorageService.guardarArchivo(ventaId, archivo));
            }
            return storedUploads;
        } catch (RuntimeException exception) {
            cleanupStoredFilesImmediately(storedUploads.stream()
                    .map(VentaImagenStorageService.StoredVentaUpload::storedPath)
                    .toList());
            throw exception;
        }
    }

    private void normalizeArchivoOrder(Venta venta) {
        List<VentaArchivoDiseno> orderedFiles = venta.getArchivosDiseno().stream()
                .sorted(Comparator.comparing(VentaArchivoDiseno::getOrdenVisual)
                        .thenComparing(VentaArchivoDiseno::getFechaRegistro, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VentaArchivoDiseno::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (int index = 0; index < orderedFiles.size(); index++) {
            orderedFiles.get(index).setOrdenVisual(index);
        }
    }

    private void syncLegacyImageUrl(Venta venta, boolean fileStateChanged) {
        if (!venta.getArchivosDiseno().isEmpty()) {
            VentaArchivoDiseno primaryFile = venta.getArchivosDiseno().stream()
                    .sorted(Comparator.comparing(VentaArchivoDiseno::getOrdenVisual).thenComparing(VentaArchivoDiseno::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst()
                    .orElse(null);
            venta.setImagenDisenoUrl(primaryFile == null ? null : primaryFile.getRutaAlmacenamiento());
            return;
        }

        if (fileStateChanged) {
            venta.setImagenDisenoUrl(null);
        }
    }

    private void cleanupStoredFilesImmediately(List<String> storedPaths) {
        storedPaths.stream()
                .filter(this::hasText)
                .forEach(path -> {
                    try {
                        ventaImagenStorageService.eliminarImagen(path);
                    } catch (RuntimeException ignored) {
                        // Best-effort cleanup of files created during a failed mutation.
                    }
                });
    }

    private void registerFileSynchronization(List<String> newStoredPaths, List<String> storedPathsToDelete) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storedPathsToDelete.forEach(ventaImagenStorageService::eliminarImagen);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storedPathsToDelete.forEach(ventaImagenStorageService::eliminarImagen);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cleanupStoredFilesImmediately(newStoredPaths);
                }
            }
        });
    }

    private void validarRequest(VentaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de venta es obligatoria.");
        }
        if (request.productoId() == null) {
            throw new IllegalArgumentException("El producto es obligatorio.");
        }
        if (normalizarTexto(request.codigoVendido()) == null) {
            throw new IllegalArgumentException("El codigo vendido es obligatorio.");
        }
        if (request.cantidad() == null || request.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (request.precioUnitario() == null || request.precioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser mayor a cero.");
        }
        if (request.totalVenta() == null || request.totalVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El total de la venta debe ser mayor a cero.");
        }
        if (normalizarTexto(request.clienteNombre()) == null) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        if (normalizarTexto(request.clienteTelefono()) == null) {
            throw new IllegalArgumentException("El telefono del cliente es obligatorio.");
        }
        if (request.metodoPago() == null) {
            throw new IllegalArgumentException("El metodo de pago es obligatorio.");
        }
        if (request.fechaPago() == null) {
            throw new IllegalArgumentException("La fecha de pago es obligatoria.");
        }
        if (request.montoPagado() == null || request.montoPagado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto pagado debe ser mayor a cero.");
        }
        if (request.estadoPedido() == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio.");
        }
        validarEstadoActivo(request.estadoPedido());
        if (request.fechaVenta() == null) {
            throw new IllegalArgumentException("La fecha de venta es obligatoria.");
        }
        if (request.valorEnvio() != null && request.valorEnvio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor de envio no puede ser negativo.");
        }
    }

    private void validarEstadoActivo(VentaEstadoPedido estadoPedido) {
        if (!ESTADOS_ACTIVOS.contains(estadoPedido)) {
            throw new IllegalArgumentException("El estado del pedido no es valido para esta etapa.");
        }
    }

    private Venta obtenerEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }

    private VentaResponse toResponse(Venta venta) {
        List<VentaArchivoDisenoResponse> archivos = buildArchivosResponse(venta);

        return new VentaResponse(
                venta.getId(),
                venta.getProductoId(),
                venta.getCodigoProductoBase(),
                venta.getNombreProductoBase(),
                venta.getTipoProducto(),
                venta.getCodigoVendido(),
                venta.getColeccionDiseno(),
                venta.getReferenciaDiseno(),
                venta.getImagenDisenoUrl(),
                archivos,
                venta.getCantidad(),
                venta.getPrecioUnitario(),
                venta.getTotalVenta(),
                venta.getClienteNombre(),
                venta.getClienteApellidos(),
                venta.getClienteRut(),
                venta.getClienteTelefono(),
                venta.getClienteEmail(),
                venta.getClienteDireccion(),
                venta.getReferenciasDireccion(),
                venta.getClienteComuna(),
                venta.getValorEnvio(),
                venta.getMetodoPago(),
                venta.getFechaPago(),
                venta.getMontoPagado(),
                venta.getEstadoPedido(),
                venta.getCostoMaterialesSnapshot(),
                venta.getCostoReposicionSnapshot(),
                venta.getCostoTotalSnapshot(),
                venta.getValorVentaSnapshot(),
                venta.getPrecioSugeridoSnapshot(),
                venta.getGananciaDirectaSnapshot(),
                venta.getFechaVenta(),
                venta.getCreatedAt(),
                venta.getUpdatedAt());
    }

    private List<VentaArchivoDisenoResponse> buildArchivosResponse(Venta venta) {
        List<VentaArchivoDiseno> archivosPersistidos = venta.getArchivosDiseno().stream()
                .sorted(Comparator.comparing(VentaArchivoDiseno::getOrdenVisual).thenComparing(VentaArchivoDiseno::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (!archivosPersistidos.isEmpty()) {
            return archivosPersistidos.stream()
                    .map(archivo -> new VentaArchivoDisenoResponse(
                            archivo.getId(),
                            archivo.getNombreOriginal(),
                            archivo.getTipoMime(),
                            archivo.getTamanoBytes(),
                            archivo.getOrdenVisual(),
                            archivo.getFechaRegistro(),
                            "/api/ventas/" + venta.getId() + "/archivos/" + archivo.getId(),
                            "/api/ventas/" + venta.getId() + "/archivos/" + archivo.getId() + "?download=true"))
                    .toList();
        }

        if (!hasText(venta.getImagenDisenoUrl())) {
            return List.of();
        }

        String fileName = extractFileName(venta.getImagenDisenoUrl());
        return List.of(new VentaArchivoDisenoResponse(
                null,
                fileName,
                resolveMimeTypeFromPath(venta.getImagenDisenoUrl()),
                0L,
                0,
                venta.getUpdatedAt(),
                "/api/ventas/" + venta.getId() + "/archivo-diseno",
                "/api/ventas/" + venta.getId() + "/archivo-diseno?download=true"));
    }

    private String extractFileName(String storedPath) {
        String normalizedPath = storedPath.replace('\\', '/');
        int separatorIndex = normalizedPath.lastIndexOf('/');
        return separatorIndex >= 0 ? normalizedPath.substring(separatorIndex + 1) : normalizedPath;
    }

    private String resolveMimeTypeFromPath(String storedPath) {
        String normalizedPath = storedPath.toLowerCase();
        if (normalizedPath.endsWith(".png")) {
            return "image/png";
        }
        if (normalizedPath.endsWith(".jpg") || normalizedPath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalizedPath.endsWith(".webp")) {
            return "image/webp";
        }
        if (normalizedPath.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private BigDecimal valorOZero(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String normalizarTexto(String value) {
        if (value == null) {
            return null;
        }

        String normalizado = value.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private String normalizarTextoObligatorio(String value) {
        return normalizarTexto(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
