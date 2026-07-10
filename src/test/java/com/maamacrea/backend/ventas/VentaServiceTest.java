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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private VentaService ventaService;

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
    }

    @Test
    void listaUltimasVentasEnOrdenDescendente() {
        Venta venta = new Venta();
        venta.setId(9L);
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
        Venta venta = new Venta();
        venta.setId(9L);
        venta.setEstadoPedido(VentaEstadoPedido.PAGO_CONFIRMADO);

        when(ventaRepository.findById(9L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VentaResponse response = ventaService.actualizarEstado(
                9L, new VentaEstadoUpdateRequest(VentaEstadoPedido.ENVIO));

        assertThat(response.estadoPedido()).isEqualTo(VentaEstadoPedido.ENVIO);
        assertThat(venta.getEstadoPedido()).isEqualTo(VentaEstadoPedido.ENVIO);
    }
}
