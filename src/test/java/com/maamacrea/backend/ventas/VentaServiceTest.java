package com.maamacrea.backend.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maamacrea.backend.productos.ProductoResponse;
import com.maamacrea.backend.productos.ProductoService;
import com.maamacrea.backend.productos.ProductoTipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private VentaArchivoDisenoRepository ventaArchivoDisenoRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private VentaImagenStorageService ventaImagenStorageService;

    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        ventaService = new VentaService(
                ventaRepository,
                ventaArchivoDisenoRepository,
                productoService,
                ventaImagenStorageService,
                10);
    }

    @Test
    void creaVentaGuardandoSnapshotDelCosteoActual() {
        ProductoResponse producto = new ProductoResponse(
                1L,
                "COJ-PER-001",
                "Cojin Personalizado 40x40",
                ProductoTipo.COJIN_PERSONALIZADO,
                LocalDateTime.of(2026, 7, 9, 10, 0),
                new BigDecimal("9990.00"),
                new BigDecimal("1150.0000"),
                new BigDecimal("4600.00"),
                new BigDecimal("2300.00"),
                null,
                false,
                true,
                List.of(),
                List.of(),
                List.of());

        when(productoService.buscarPorId(1L)).thenReturn(producto);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            venta.setId(50L);
            return venta;
        });

        VentaResponse response = ventaService.crear(new VentaRequest(
                1L,
                "COJ-AMO-001",
                "COJ-AMO",
                "Cojin amor pareja modelo 001",
                new BigDecimal("1"),
                new BigDecimal("9990"),
                new BigDecimal("9990"),
                "Camila",
                "Perez",
                null,
                "+56912345678",
                null,
                null,
                "Dejar en conserjeria",
                "Santiago",
                new BigDecimal("0"),
                VentaMetodoPago.TRANSFERENCIA,
                LocalDate.of(2026, 7, 10),
                new BigDecimal("9990"),
                VentaEstadoPedido.PAGO_CONFIRMADO,
                LocalDate.of(2026, 7, 10)));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());

        assertThat(captor.getValue().getCostoMaterialesSnapshot()).isEqualByComparingTo("1150.0000");
        assertThat(captor.getValue().getCostoReposicionSnapshot()).isEqualByComparingTo("1150.0000");
        assertThat(captor.getValue().getCostoTotalSnapshot()).isEqualByComparingTo("1150.0000");
        assertThat(captor.getValue().getValorVentaSnapshot()).isEqualByComparingTo("9990.00");
        assertThat(captor.getValue().getGananciaDirectaSnapshot()).isEqualByComparingTo("8840.00");
        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.codigoProductoBase()).isEqualTo("COJ-PER-001");
        assertThat(response.codigoVendido()).isEqualTo("COJ-AMO-001");
        assertThat(response.coleccionDiseno()).isEqualTo("COJ-AMO");
        assertThat(response.valorVentaSnapshot()).isEqualByComparingTo("9990.00");
        assertThat(response.archivos()).isEmpty();
    }

    @Test
    void listaUltimasVentasEnOrdenDescendente() {
        Venta venta = createVentaBase(9L);

        when(ventaRepository.findAllByOrderByFechaVentaDescCreatedAtDescIdDesc(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(venta));

        List<VentaResponse> response = ventaService.listarUltimas(5);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).codigoVendido()).isEqualTo("COJ-AMO-001");
    }

    @Test
    void fallaSiFaltaCodigoVendido() {
        assertThatThrownBy(() -> ventaService.crear(new VentaRequest(
                        1L,
                        "   ",
                        null,
                        null,
                        new BigDecimal("1"),
                        new BigDecimal("9990"),
                        new BigDecimal("9990"),
                        "Camila",
                        null,
                        null,
                        "+56912345678",
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.ZERO,
                        VentaMetodoPago.TRANSFERENCIA,
                        LocalDate.of(2026, 7, 10),
                        new BigDecimal("9990"),
                        VentaEstadoPedido.PAGO_CONFIRMADO,
                        LocalDate.of(2026, 7, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El codigo vendido es obligatorio.");
    }

    @Test
    void actualizaEstadoDeVentaExistente() {
        Venta venta = createVentaBase(9L);
        venta.setEstadoPedido(VentaEstadoPedido.PAGO_CONFIRMADO);

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.actualizarEstado(
                9L, new VentaEstadoUpdateRequest(VentaEstadoPedido.ENTREGADO_A_CORREOS));

        assertThat(response.estadoPedido()).isEqualTo(VentaEstadoPedido.ENTREGADO_A_CORREOS);
        assertThat(venta.getEstadoPedido()).isEqualTo(VentaEstadoPedido.ENTREGADO_A_CORREOS);
    }

    @Test
    void permiteMarcarVentaComoFinalizada() {
        Venta venta = createVentaBase(9L);
        venta.setEstadoPedido(VentaEstadoPedido.RECIBIDO_POR_CLIENTE);

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.actualizarEstado(
                9L, new VentaEstadoUpdateRequest(VentaEstadoPedido.VENTA_FINALIZADA));

        assertThat(response.estadoPedido()).isEqualTo(VentaEstadoPedido.VENTA_FINALIZADA);
        assertThat(venta.getEstadoPedido()).isEqualTo(VentaEstadoPedido.VENTA_FINALIZADA);
    }

    @Test
    void actualizaVentaExistenteSinCambiarSnapshots() {
        Venta venta = createVentaBase(9L);
        venta.setCostoMaterialesSnapshot(new BigDecimal("1150.0000"));
        venta.setCostoReposicionSnapshot(new BigDecimal("1150.0000"));
        venta.setCostoTotalSnapshot(new BigDecimal("1150.0000"));
        venta.setValorVentaSnapshot(new BigDecimal("9990.00"));
        venta.setGananciaDirectaSnapshot(new BigDecimal("8840.00"));

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.actualizar(
                9L,
                new VentaRequest(
                        1L,
                        "COJ-AMO-002",
                        "COJ-AMO",
                        "Cojin amor pareja modelo 002",
                        new BigDecimal("2"),
                        new BigDecimal("10990"),
                        new BigDecimal("21980"),
                        "Camila",
                        "Perez",
                        null,
                        "+56912345678",
                        null,
                        "Pasaje 123",
                        "Casa azul, porton negro",
                        "Santiago",
                        new BigDecimal("3120"),
                        VentaMetodoPago.MERCADO_PAGO,
                        LocalDate.of(2026, 7, 11),
                        new BigDecimal("21980"),
                        VentaEstadoPedido.DISENO_Y_CONFECCION,
                        LocalDate.of(2026, 7, 11)));

        assertThat(response.codigoVendido()).isEqualTo("COJ-AMO-002");
        assertThat(response.referenciasDireccion()).isEqualTo("Casa azul, porton negro");
        assertThat(response.totalVenta()).isEqualByComparingTo("21980.00");
        assertThat(venta.getCostoMaterialesSnapshot()).isEqualByComparingTo("1150.0000");
        assertThat(venta.getCostoReposicionSnapshot()).isEqualByComparingTo("1150.0000");
    }

    @Test
    void rechazaEstadoNoActivoEnActualizacion() {
        assertThatThrownBy(() -> ventaService.actualizarEstado(
                        9L, new VentaEstadoUpdateRequest(VentaEstadoPedido.CANCELADO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El estado del pedido no es valido para esta etapa.");
    }

    @Test
    void eliminaVentaYTodosSusArchivosAsociados() {
        Venta venta = createVentaBase(9L);
        venta.setImagenDisenoUrl("uploads/ventas/9/archivo-principal.webp");

        VentaArchivoDiseno archivo = new VentaArchivoDiseno();
        archivo.setId(41L);
        archivo.setRutaAlmacenamiento("uploads/ventas/9/archivo-principal.webp");
        venta.addArchivoDiseno(archivo);

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));

        ventaService.eliminar(9L);

        verify(ventaRepository).delete(venta);
    }

    @Test
    void agregaMultiplesArchivosYMantieneLaRutaLegacyDelPrimero() {
        Venta venta = createVentaBase(10L);
        venta.setCodigoVendido("COJ-PER-001");

        MockMultipartFile frente =
                new MockMultipartFile("archivos", "frente.png", "image/png", new byte[] {1, 2, 3});
        MockMultipartFile posterior =
                new MockMultipartFile("archivos", "posterior.jpg", "image/jpeg", new byte[] {4, 5, 6});

        when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));
        when(ventaImagenStorageService.guardarArchivo(10L, frente))
                .thenReturn(new VentaImagenStorageService.StoredVentaUpload(
                        "frente.png",
                        "uuid-frente.png",
                        "uploads/ventas/10/uuid-frente.png",
                        "image/png",
                        3L));
        when(ventaImagenStorageService.guardarArchivo(10L, posterior))
                .thenReturn(new VentaImagenStorageService.StoredVentaUpload(
                        "posterior.jpg",
                        "uuid-posterior.jpg",
                        "uploads/ventas/10/uuid-posterior.jpg",
                        "image/jpeg",
                        3L));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.agregarArchivos(10L, List.of(frente, posterior));

        assertThat(venta.getArchivosDiseno()).hasSize(2);
        assertThat(venta.getImagenDisenoUrl()).isEqualTo("uploads/ventas/10/uuid-frente.png");
        assertThat(response.archivos()).hasSize(2);
        assertThat(response.archivos().get(0).nombreOriginal()).isEqualTo("frente.png");
        assertThat(response.archivos().get(1).nombreOriginal()).isEqualTo("posterior.jpg");
    }

    @Test
    void eliminaUnArchivoIndividualYMantieneLosRestantes() {
        Venta venta = createVentaBase(11L);
        venta.setImagenDisenoUrl("uploads/ventas/11/uuid-frente.png");

        VentaArchivoDiseno frente = new VentaArchivoDiseno();
        frente.setId(51L);
        frente.setNombreOriginal("frente.png");
        frente.setNombreAlmacenado("uuid-frente.png");
        frente.setRutaAlmacenamiento("uploads/ventas/11/uuid-frente.png");
        frente.setTipoMime("image/png");
        frente.setTamanoBytes(3L);
        frente.setOrdenVisual(0);

        VentaArchivoDiseno posterior = new VentaArchivoDiseno();
        posterior.setId(52L);
        posterior.setNombreOriginal("posterior.png");
        posterior.setNombreAlmacenado("uuid-posterior.png");
        posterior.setRutaAlmacenamiento("uploads/ventas/11/uuid-posterior.png");
        posterior.setTipoMime("image/png");
        posterior.setTamanoBytes(3L);
        posterior.setOrdenVisual(1);

        venta.addArchivoDiseno(frente);
        venta.addArchivoDiseno(posterior);

        when(ventaRepository.findById(11L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.eliminarArchivo(11L, 51L);

        assertThat(venta.getArchivosDiseno()).hasSize(1);
        assertThat(venta.getArchivosDiseno().get(0).getId()).isEqualTo(52L);
        assertThat(venta.getImagenDisenoUrl()).isEqualTo("uploads/ventas/11/uuid-posterior.png");
        assertThat(response.archivos()).hasSize(1);
        assertThat(response.archivos().get(0).nombreOriginal()).isEqualTo("posterior.png");
    }

    @Test
    void mantieneCompatibilidadConRutaLegacyCuandoNoHayRegistrosMigrados() {
        Venta venta = createVentaBase(12L);
        venta.setImagenDisenoUrl("uploads/ventas/disenos/COJ-PER-001-20260711-204626.png");

        when(ventaRepository.findById(12L)).thenReturn(Optional.of(venta));

        VentaResponse response = ventaService.buscarPorId(12L);

        assertThat(response.archivos()).hasSize(1);
        assertThat(response.archivos().get(0).id()).isNull();
        assertThat(response.archivos().get(0).urlVisualizacion()).isEqualTo("/api/ventas/12/archivo-diseno");
    }

    private Venta createVentaBase(Long id) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setProductoId(1L);
        venta.setCodigoProductoBase("COJ-PER-001");
        venta.setNombreProductoBase("Cojin Personalizado 40x40");
        venta.setTipoProducto(ProductoTipo.COJIN_PERSONALIZADO);
        venta.setCodigoVendido("COJ-AMO-001");
        venta.setColeccionDiseno("COJ-AMO");
        venta.setCantidad(new BigDecimal("1.000"));
        venta.setPrecioUnitario(new BigDecimal("9990.00"));
        venta.setTotalVenta(new BigDecimal("9990.00"));
        venta.setClienteNombre("Camila");
        venta.setClienteTelefono("+56912345678");
        venta.setValorEnvio(new BigDecimal("0.00"));
        venta.setMetodoPago(VentaMetodoPago.TRANSFERENCIA);
        venta.setFechaPago(LocalDate.of(2026, 7, 10));
        venta.setMontoPagado(new BigDecimal("9990.00"));
        venta.setEstadoPedido(VentaEstadoPedido.PAGO_CONFIRMADO);
        venta.setCostoMaterialesSnapshot(new BigDecimal("1150.0000"));
        venta.setCostoReposicionSnapshot(new BigDecimal("1150.0000"));
        venta.setCostoTotalSnapshot(new BigDecimal("1150.0000"));
        venta.setValorVentaSnapshot(new BigDecimal("9990.00"));
        venta.setGananciaDirectaSnapshot(new BigDecimal("8840.00"));
        venta.setFechaVenta(LocalDate.of(2026, 7, 10));
        venta.setCreatedAt(LocalDateTime.of(2026, 7, 10, 12, 0));
        venta.setUpdatedAt(LocalDateTime.of(2026, 7, 10, 12, 0));
        return venta;
    }
}