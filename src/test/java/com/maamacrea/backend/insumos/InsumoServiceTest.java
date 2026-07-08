package com.maamacrea.backend.insumos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsumoServiceTest {

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InsumoCompraRepository insumoCompraRepository;

    @Mock
    private InsumoDocumentoStorageService insumoDocumentoStorageService;

    @InjectMocks
    private InsumoService insumoService;

    @Test
    void creaInsumoGenerandoCompraInicialVigente() {
        AtomicReference<InsumoCompra> compraGuardada = new AtomicReference<>();

        when(insumoRepository.existsByCodigoProductoIgnoreCase("ALG-001")).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> {
            Insumo insumo = invocation.getArgument(0);
            if (insumo.getId() == null) {
                insumo.setId(1L);
            }
            return insumo;
        });
        when(insumoCompraRepository.save(any(InsumoCompra.class))).thenAnswer(invocation -> {
            InsumoCompra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(10L);
            }
            compraGuardada.set(compra);
            return compra;
        });
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> Optional.ofNullable(compraGuardada.get()));
        when(insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> compraGuardada.get() == null ? List.of() : List.of(compraGuardada.get()));

        InsumoResponse response = insumoService.crear(new InsumoRequest(
                "alg-001",
                "Algodon sintetico",
                "MATERIALES_TEXTILES",
                "kg",
                new BigDecimal("3"),
                null,
                null,
                new BigDecimal("1680"),
                new BigDecimal("320"),
                new BigDecimal("2000"),
                "Proveedor Base",
                LocalDate.of(2026, 7, 8),
                "FACTURA",
                "F-001",
                null,
                "Compra inicial"));

        ArgumentCaptor<InsumoCompra> captor = ArgumentCaptor.forClass(InsumoCompra.class);
        verify(insumoCompraRepository).save(captor.capture());

        assertThat(captor.getValue().isVigente()).isTrue();
        assertThat(captor.getValue().getPrecioUnitario()).isEqualByComparingTo("666.6667");
        assertThat(response.compraVigenteId()).isEqualTo(10L);
        assertThat(response.totalCompras()).isEqualTo(1);
    }

    @Test
    void actualizarConCambioPrecioCreaNuevaCompraSinPerderHistorial() {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setCodigoProducto("ALG-001");
        insumo.setNombre("Algodon sintetico");
        insumo.setCategoria("MATERIALES_TEXTILES");
        insumo.setProveedor("Proveedor Base");

        InsumoCompra compraAnterior = new InsumoCompra();
        compraAnterior.setId(10L);
        compraAnterior.setInsumo(insumo);
        compraAnterior.setFechaCompra(LocalDate.of(2026, 7, 1));
        compraAnterior.setCantidadComprada(new BigDecimal("3.000"));
        compraAnterior.setUnidadMedida("kg");
        compraAnterior.setPrecioCompraTotal(new BigDecimal("2000.00"));
        compraAnterior.setPrecioUnitario(new BigDecimal("666.6667"));
        compraAnterior.setVigente(true);

        AtomicReference<InsumoCompra> compraActual = new AtomicReference<>(compraAnterior);

        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.existsByCodigoProductoIgnoreCaseAndIdNot("ALG-001", 1L)).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> Optional.ofNullable(compraActual.get()));
        when(insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenAnswer(invocation -> List.of(compraActual.get(), compraAnterior));
        when(insumoCompraRepository.save(any(InsumoCompra.class))).thenAnswer(invocation -> {
            InsumoCompra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(11L);
            }
            compraActual.set(compra);
            return compra;
        });

        InsumoResponse response = insumoService.actualizar(1L, new InsumoRequest(
                "ALG-001",
                "Algodon sintetico premium",
                "MATERIALES_TEXTILES",
                "kg",
                new BigDecimal("3"),
                null,
                null,
                new BigDecimal("3360"),
                new BigDecimal("640"),
                new BigDecimal("4000"),
                "Proveedor Base",
                LocalDate.of(2026, 7, 8),
                "FACTURA",
                "F-002",
                null,
                "Compra nueva"));

        verify(insumoCompraRepository).clearVigenteByInsumoId(1L);
        assertThat(compraActual.get().getId()).isEqualTo(11L);
        assertThat(compraActual.get().getPrecioCompraTotal()).isEqualByComparingTo("4000.00");
        assertThat(compraActual.get().isVigente()).isTrue();
        assertThat(response.tieneCambioPrecio()).isTrue();
        assertThat(response.ultimoCambioPrecio()).isEqualTo(LocalDate.of(2026, 7, 8));
    }
}
