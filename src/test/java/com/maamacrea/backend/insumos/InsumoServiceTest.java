package com.maamacrea.backend.insumos;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maamacrea.backend.ApiRequestException;
import com.maamacrea.backend.productos.ProductoInsumoRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InsumoServiceTest {

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InsumoCompraRepository insumoCompraRepository;

    @Mock
    private ProductoInsumoRepository productoInsumoRepository;

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
                null,
                null,
                null,
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
                null,
                null,
                null,
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

    @Test
    void guardaCantidadMlCompradosEnInsumoDeTinta() {
        AtomicReference<InsumoCompra> compraGuardada = new AtomicReference<>();

        when(insumoRepository.existsByCodigoProductoIgnoreCase("T49M1")).thenReturn(false);
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
                "T49M1",
                "Tinta Negra Sublimacion Epson",
                "SUBLIMACION",
                "ml",
                new BigDecimal("1"),
                new BigDecimal("140"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("19990"),
                "Proveedor Tinta",
                LocalDate.of(2026, 7, 8),
                "FACTURA",
                "F-100",
                null,
                "Tinta negra"));

        assertThat(response.cantidadMlComprados()).isEqualByComparingTo("140.0000");
        assertThat(compraGuardada.get().getCantidadMlComprados()).isEqualByComparingTo("140.0000");
        assertThat(compraGuardada.get().getPrecioUnitario()).isEqualByComparingTo("142.7857");
    }

    @Test
    void guardaContenidoDeConoConConversionAYCostoPorMetro() {
        AtomicReference<Insumo> primerInsumoGuardado = new AtomicReference<>();
        AtomicReference<InsumoCompra> compraGuardada = new AtomicReference<>();

        when(insumoRepository.existsByCodigoProductoIgnoreCase("CON-HIL-BLA-001")).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> {
            Insumo insumo = invocation.getArgument(0);
            if (primerInsumoGuardado.get() == null) {
                Insumo snapshot = new Insumo();
                snapshot.setCantidadComprada(insumo.getCantidadComprada());
                snapshot.setContenidoPorUnidad(insumo.getContenidoPorUnidad());
                snapshot.setUnidadContenido(insumo.getUnidadContenido());
                snapshot.setContenidoTotalComprado(insumo.getContenidoTotalComprado());
                snapshot.setPrecioCompraTotal(insumo.getPrecioCompraTotal());
                snapshot.setCostoUnitario(insumo.getCostoUnitario());
                primerInsumoGuardado.set(snapshot);
            }
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
                "CON-HIL-BLA-001",
                "Cono Hilo Blanco",
                "COSTURA_Y_CONFECCION",
                "cono",
                new BigDecimal("9"),
                null,
                new BigDecimal("2000"),
                "yd",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("18000"),
                "Proveedor Hilo",
                LocalDate.of(2026, 7, 9),
                "FACTURA",
                "F-300",
                null,
                "Hilo blanco"));

        assertThat(primerInsumoGuardado.get()).isNotNull();
        assertThat(primerInsumoGuardado.get().getCantidadComprada()).isEqualByComparingTo("9.000");
        assertThat(primerInsumoGuardado.get().getContenidoPorUnidad()).isEqualByComparingTo("2000.0000");
        assertThat(primerInsumoGuardado.get().getUnidadContenido()).isEqualTo("yd");
        assertThat(primerInsumoGuardado.get().getContenidoTotalComprado()).isEqualByComparingTo("16459.2000");
        assertThat(primerInsumoGuardado.get().getPrecioCompraTotal()).isEqualByComparingTo("18000.00");
        assertThat(primerInsumoGuardado.get().getCostoUnitario()).isEqualByComparingTo("1.0936");
        assertThat(response.contenidoPorUnidad()).isEqualByComparingTo("2000.0000");
        assertThat(response.unidadContenido()).isEqualTo("yd");
        assertThat(response.contenidoTotalComprado()).isEqualByComparingTo("16459.2000");
        assertThat(compraGuardada.get().getPrecioUnitario()).isEqualByComparingTo("1.0936");
    }

    @Test
    void eliminaInsumoSinDependencias() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.existsByInsumoId(34L)).thenReturn(false);

        insumoService.eliminar(34L);

        verify(insumoRepository).delete(insumo);
        verify(insumoRepository).flush();
    }

    @Test
    void rechazaEliminacionCuandoExisteRelacionConProductos() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.existsByInsumoId(34L)).thenReturn(true);

        assertThatThrownBy(() -> insumoService.eliminar(34L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("No se puede eliminar este insumo porque tiene movimientos o registros asociados. Puedes desactivarlo en lugar de eliminarlo.")
                .satisfies(exception -> {
                    ApiRequestException apiException = (ApiRequestException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("INSUMO_CON_DEPENDENCIAS");
                });

        verify(insumoRepository, never()).delete(any(Insumo.class));
        verify(insumoRepository, never()).flush();
    }

    @Test
    void transformaViolacionDeIntegridadEnConflictoControlado() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.existsByInsumoId(34L)).thenReturn(false);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk_producto_insumos_insumo"))
                .when(insumoRepository)
                .flush();

        assertThatThrownBy(() -> insumoService.eliminar(34L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("No se puede eliminar este insumo porque tiene movimientos o registros asociados. Puedes desactivarlo en lugar de eliminarlo.")
                .satisfies(exception -> {
                    ApiRequestException apiException = (ApiRequestException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("INSUMO_CON_DEPENDENCIAS");
                });

        verify(insumoRepository).delete(insumo);
        verify(insumoRepository).flush();
    }
}
