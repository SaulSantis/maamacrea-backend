package com.maamacrea.backend.ventas;

import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.productos.ProductoResponse;
import com.maamacrea.backend.productos.ProductoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final VentaImagenStorageService ventaImagenStorageService;

    public VentaService(
            VentaRepository ventaRepository,
            ProductoService productoService,
            VentaImagenStorageService ventaImagenStorageService) {
        this.ventaRepository = ventaRepository;
        this.productoService = productoService;
        this.ventaImagenStorageService = ventaImagenStorageService;
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
    public VentaResponse actualizarEstado(Long id, VentaEstadoUpdateRequest request) {
        if (request == null || request.estadoPedido() == null) {
            throw new IllegalArgumentException("El estado del pedido es obligatorio.");
        }

        Venta venta = obtenerEntidad(id);
        venta.setEstadoPedido(request.estadoPedido());
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponse actualizarImagenDiseno(Long id, MultipartFile file) {
        Venta venta = obtenerEntidad(id);
        String imagenDisenoUrl = ventaImagenStorageService.guardarImagen(id, file);
        venta.setImagenDisenoUrl(imagenDisenoUrl);
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional
    public VentaResponse crear(VentaRequest request) {
        validarRequest(request);

        ProductoResponse producto = productoService.buscarPorId(request.productoId());

        BigDecimal cantidad = request.cantidad().setScale(3, RoundingMode.HALF_UP);
        BigDecimal precioUnitario = request.precioUnitario().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalVenta = request.totalVenta().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoPagado = request.montoPagado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorEnvio = valorOZero(request.valorEnvio(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
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
        venta.setClienteComuna(normalizarTexto(request.clienteComuna()));
        venta.setValorEnvio(valorEnvio);
        venta.setMetodoPago(request.metodoPago());
        venta.setFechaPago(request.fechaPago());
        venta.setMontoPagado(montoPagado);
        venta.setEstadoPedido(request.estadoPedido());
        venta.setCostoMaterialesSnapshot(costoMateriales);
        venta.setCostoReposicionSnapshot(costoReposicion);
        venta.setCostoTotalSnapshot(costoTotal);
        venta.setValorVentaSnapshot(valorVentaSnapshot);
        venta.setPrecioSugeridoSnapshot(precioSugerido);
        venta.setGananciaDirectaSnapshot(gananciaDirecta);
        venta.setFechaVenta(request.fechaVenta());

        return toResponse(ventaRepository.save(venta));
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
        if (request.fechaVenta() == null) {
            throw new IllegalArgumentException("La fecha de venta es obligatoria.");
        }
        if (request.valorEnvio() != null && request.valorEnvio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor de envio no puede ser negativo.");
        }
    }

    private Venta obtenerEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }

    private VentaResponse toResponse(Venta venta) {
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
                venta.getCantidad(),
                venta.getPrecioUnitario(),
                venta.getTotalVenta(),
                venta.getClienteNombre(),
                venta.getClienteApellidos(),
                venta.getClienteRut(),
                venta.getClienteTelefono(),
                venta.getClienteEmail(),
                venta.getClienteDireccion(),
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
}
