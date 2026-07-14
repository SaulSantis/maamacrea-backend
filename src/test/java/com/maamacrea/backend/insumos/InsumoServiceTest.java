package com.maamacrea.backend.insumos;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maamacrea.backend.ApiRequestException;
import com.maamacrea.backend.productos.Producto;
import com.maamacrea.backend.productos.ProductoInsumoRepository;
import com.maamacrea.backend.productos.ProductoService;
import com.maamacrea.backend.productos.ProductoInsumo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private ProductoService productoService;

    @Mock
    private InsumoDocumentoStorageService insumoDocumentoStorageService;

    @InjectMocks
    private InsumoService insumoService;

    @Test
    void creaInsumoBaseSinCompraInicial() {
        when(insumoRepository.existsByCodigoProductoIgnoreCase("ALG-001")).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> {
            Insumo insumo = invocation.getArgument(0);
            if (insumo.getId() == null) {
                insumo.setId(1L);
            }
            return insumo;
        });
        when(insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenReturn(List.of());

        InsumoResponse response = insumoService.crear(new CreateInsumoRequest(
                "alg-001",
                "Algodon sintetico",
                "MATERIALES_TEXTILES"));

        verify(insumoCompraRepository, never()).save(any(InsumoCompra.class));
        assertThat(response.codigoProducto()).isEqualTo("ALG-001");
        assertThat(response.nombre()).isEqualTo("Algodon sintetico");
        assertThat(response.compraVigenteId()).isNull();
        assertThat(response.totalCompras()).isEqualTo(0);
        assertThat(response.precioCompraTotal()).isNull();
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
    void registraCompraDeTintaYGuardaCantidadMlComprados() {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setCodigoProducto("T49M1");
        insumo.setNombre("Tinta Negra Sublimacion Epson");
        insumo.setCategoria("SUBLIMACION");

        AtomicReference<InsumoCompra> compraGuardada = new AtomicReference<>();

        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(insumoCompraRepository.save(any(InsumoCompra.class))).thenAnswer(invocation -> {
            InsumoCompra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(10L);
            }
            compraGuardada.set(compra);
            return compra;
        });
        InsumoCompraResponse response = insumoService.registrarCompra(1L, new InsumoCompraRequest(
                LocalDate.of(2026, 7, 8),
                new BigDecimal("1"),
                new BigDecimal("140"),
                null,
                null,
                "ml",
                new BigDecimal("19990"),
                null,
                null,
                null,
                null,
                null,
                null,
                "FACTURA",
                "F-100",
                null,
                "Tinta negra"));

        assertThat(response.cantidadMlComprados()).isEqualByComparingTo("140.0000");
        assertThat(compraGuardada.get().getCantidadMlComprados()).isEqualByComparingTo("140.0000");
        assertThat(compraGuardada.get().getPrecioUnitario()).isEqualByComparingTo("142.7857");
    }

    @Test
    void registraCompraDeConoConConversionAYCostoPorMetro() {
        Insumo insumo = new Insumo();
        insumo.setId(1L);
        insumo.setCodigoProducto("CON-HIL-BLA-001");
        insumo.setNombre("Cono Hilo Blanco");
        insumo.setCategoria("COSTURA_Y_CONFECCION");

        AtomicReference<Insumo> insumoActualizado = new AtomicReference<>();
        AtomicReference<InsumoCompra> compraGuardada = new AtomicReference<>();

        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumo));
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(invocation -> {
            Insumo entidad = invocation.getArgument(0);
            Insumo snapshot = new Insumo();
            snapshot.setCantidadComprada(entidad.getCantidadComprada());
            snapshot.setContenidoPorUnidad(entidad.getContenidoPorUnidad());
            snapshot.setUnidadContenido(entidad.getUnidadContenido());
            snapshot.setContenidoTotalComprado(entidad.getContenidoTotalComprado());
            snapshot.setPrecioCompraTotal(entidad.getPrecioCompraTotal());
            snapshot.setCostoUnitario(entidad.getCostoUnitario());
            insumoActualizado.set(snapshot);
            return entidad;
        });
        when(insumoCompraRepository.save(any(InsumoCompra.class))).thenAnswer(invocation -> {
            InsumoCompra compra = invocation.getArgument(0);
            if (compra.getId() == null) {
                compra.setId(10L);
            }
            compraGuardada.set(compra);
            return compra;
        });
        InsumoCompraResponse response = insumoService.registrarCompra(1L, new InsumoCompraRequest(
                LocalDate.of(2026, 7, 9),
                new BigDecimal("9"),
                null,
                new BigDecimal("2000"),
                "yd",
                "cono",
                new BigDecimal("18000"),
                null,
                null,
                null,
                null,
                null,
                null,
                "FACTURA",
                "F-300",
                null,
                "Hilo blanco"));

        assertThat(insumoActualizado.get()).isNotNull();
        assertThat(insumoActualizado.get().getCantidadComprada()).isEqualByComparingTo("9.000");
        assertThat(insumoActualizado.get().getContenidoPorUnidad()).isEqualByComparingTo("2000.0000");
        assertThat(insumoActualizado.get().getUnidadContenido()).isEqualTo("yd");
        assertThat(insumoActualizado.get().getContenidoTotalComprado()).isEqualByComparingTo("16459.2000");
        assertThat(insumoActualizado.get().getPrecioCompraTotal()).isEqualByComparingTo("18000.00");
        assertThat(insumoActualizado.get().getCostoUnitario()).isEqualByComparingTo("1.0936");
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
        when(productoInsumoRepository.countByInsumoId(34L)).thenReturn(0L);

        insumoService.eliminar(34L);

        verify(insumoRepository).delete(insumo);
        verify(insumoRepository).flush();
    }

    @Test
    void rechazaEliminacionCuandoExisteRelacionConProductos() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.countByInsumoId(34L)).thenReturn(1L);
        when(insumoCompraRepository.countByInsumoId(34L)).thenReturn(2L);

        assertThatThrownBy(() -> insumoService.eliminar(34L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("No se puede eliminar este insumo porque tiene movimientos o registros asociados.")
                .satisfies(exception -> {
                    ApiRequestException apiException = (ApiRequestException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("INSUMO_CON_DEPENDENCIAS");
                    assertThat(apiException.getDetails()).containsEntry("productosAsociados", 1L);
                    assertThat(apiException.getDetails()).containsEntry("comprasRegistradas", 2L);
                });

        verify(insumoRepository, never()).delete(any(Insumo.class));
        verify(insumoRepository, never()).flush();
    }

    @Test
    void transformaViolacionDeIntegridadEnConflictoControlado() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.countByInsumoId(34L)).thenReturn(0L);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk_producto_insumos_insumo"))
                .when(insumoRepository)
                .flush();

        assertThatThrownBy(() -> insumoService.eliminar(34L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("No se puede eliminar este insumo porque tiene movimientos o registros asociados.")
                .satisfies(exception -> {
                    ApiRequestException apiException = (ApiRequestException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("INSUMO_CON_DEPENDENCIAS");
                });

        verify(insumoRepository).delete(insumo);
        verify(insumoRepository).flush();
    }

    @Test
    void obtieneDependenciasExternasSinBloquearPorCompras() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.countByInsumoId(34L)).thenReturn(0L);
        when(insumoCompraRepository.countByInsumoId(34L)).thenReturn(3L);

        InsumoDependenciasResponse dependencias = insumoService.obtenerDependencias(34L);

        assertThat(dependencias.tieneDependencias()).isFalse();
        assertThat(dependencias.productosAsociados()).isEqualTo(0L);
        assertThat(dependencias.comprasRegistradas()).isEqualTo(3L);
        assertThat(dependencias.impactaCosteo()).isFalse();
    }

    @Test
    void eliminaCompletoYRecalculaProductosAfectados() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        Producto producto = new Producto();
        producto.setId(99L);

        ProductoInsumo relacion = new ProductoInsumo();
        relacion.setProducto(producto);
        relacion.setInsumo(insumo);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.findByInsumoId(34L)).thenReturn(List.of(relacion));

        insumoService.eliminarCompleto(34L);

        verify(productoInsumoRepository).deleteByInsumoId(34L);
        verify(insumoRepository).delete(insumo);
        verify(insumoRepository).flush();
        verify(productoService).calcularCostos(99L);
    }

    @Test
    void informaConflictoControladoSiFallaEliminacionCompleta() {
        Insumo insumo = new Insumo();
        insumo.setId(34L);

        when(insumoRepository.findById(34L)).thenReturn(Optional.of(insumo));
        when(productoInsumoRepository.findByInsumoId(34L)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk_desconocida"))
                .when(insumoRepository)
                .flush();

        assertThatThrownBy(() -> insumoService.eliminarCompleto(34L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("No se pudo eliminar completamente el insumo porque existen registros asociados no contemplados.")
                .satisfies(exception -> {
                    ApiRequestException apiException = (ApiRequestException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("INSUMO_ELIMINACION_COMPLETA_FALLIDA");
                });
    }
}
