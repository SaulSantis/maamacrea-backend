package com.maamacrea.backend.productos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maamacrea.backend.ResourceNotFoundException;
import com.maamacrea.backend.insumos.Insumo;
import com.maamacrea.backend.insumos.InsumoCompra;
import com.maamacrea.backend.insumos.InsumoCompraRepository;
import com.maamacrea.backend.insumos.InsumoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoInsumoRepository productoInsumoRepository;

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private InsumoCompraRepository insumoCompraRepository;

    @InjectMocks
    private ProductoService productoService;

    private Insumo insumoTextil;
    private Insumo insumoSublimacion;

    @BeforeEach
    void setUp() {
        insumoTextil = new Insumo();
        insumoTextil.setId(1L);
        insumoTextil.setCodigoProducto("TEL-BIS-001");
        insumoTextil.setNombre("Tela Bistrech Blanca");
        insumoTextil.setCategoria("MATERIALES_TEXTILES");
        insumoTextil.setUnidadMedida("metro");
        insumoTextil.setCantidadComprada(new BigDecimal("10"));
        insumoTextil.setPrecioCompraTotal(new BigDecimal("5000"));
        insumoTextil.setCostoUnitario(new BigDecimal("500.0000"));

        insumoSublimacion = new Insumo();
        insumoSublimacion.setId(3L);
        insumoSublimacion.setCodigoProducto("PAP-SUB-001");
        insumoSublimacion.setNombre("Papel sublimacion");
        insumoSublimacion.setCategoria("SUBLIMACION");
        insumoSublimacion.setUnidadMedida("unidad");
        insumoSublimacion.setCantidadComprada(new BigDecimal("50"));
        insumoSublimacion.setPrecioCompraTotal(new BigDecimal("10000"));
        insumoSublimacion.setCostoUnitario(new BigDecimal("200.0000"));
    }

    @Test
    void creaProductoConUnInsumo() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-001", "Cojin 43x43");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTextil, "1.0000", "43.000", "43.000", "No aplica");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumoTextil));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-001",
                "Cojin 43x43",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        1L,
                        new BigDecimal("1"),
                        new BigDecimal("43"),
                        new BigDecimal("43"),
                        "No aplica",
                        null))));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.insumos()).hasSize(1);
        assertThat(response.insumos().get(0).medidaUsadaTexto()).isEqualTo("43 x 43 cm");
        verify(productoInsumoRepository).save(any(ProductoInsumo.class));
    }

    @Test
    void creaProductoConVariosInsumosYPermiteRepetirElMismoInsumo() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-001", "Cojin 43x43");
        ProductoInsumo primerUso = relacionPersistida(
                10L, productoGuardado, insumoTextil, "1.0000", "43.000", "43.000", "No aplica");
        ProductoInsumo segundoUso = relacionPersistida(
                11L, productoGuardado, insumoTextil, "2.0000", "43.000", "31.000", "No aplica");
        ProductoInsumo tercerUso = relacionPersistida(
                12L, productoGuardado, insumoSublimacion, "1.0000", null, null, "Pendiente");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumoTextil));
        when(insumoRepository.findById(3L)).thenReturn(Optional.of(insumoSublimacion));
        when(productoInsumoRepository.save(any(ProductoInsumo.class)))
                .thenReturn(primerUso, segundoUso, tercerUso);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L))
                .thenReturn(List.of(primerUso, segundoUso, tercerUso));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-001",
                "Cojin 43x43",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(
                        new ProductoInsumoCreateRequest(
                                1L,
                                new BigDecimal("1"),
                                new BigDecimal("43"),
                                new BigDecimal("43"),
                                "No aplica",
                                null),
                        new ProductoInsumoCreateRequest(
                                1L,
                                new BigDecimal("2"),
                                new BigDecimal("43"),
                                new BigDecimal("31"),
                                "No aplica",
                                null),
                        new ProductoInsumoCreateRequest(
                                3L,
                                new BigDecimal("1"),
                                null,
                                null,
                                "Pendiente",
                                null))));

        assertThat(response.insumos()).hasSize(3);
        assertThat(response.insumos().get(0).medidaUsadaTexto()).isEqualTo("43 x 43 cm");
        assertThat(response.insumos().get(1).medidaUsadaTexto()).isEqualTo("43 x 31 cm");
        assertThat(response.insumos().get(2).medidaUsadaTexto()).isEqualTo("No aplica");
        verify(productoInsumoRepository, times(3)).save(any(ProductoInsumo.class));
    }

    @Test
    void calculaCostoTextilUsandoCompraVigentePorArea() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-001", "Cojin 43x43");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTextil, "1.0000", "43.000", "43.000", "No aplica");
        InsumoCompra compraVigente = compraPersistida(insumoTextil, "150.000", "100.000", "5000.00");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(1L);
            }
            return producto;
        });
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumoTextil));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.of(compraVigente));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-001",
                "Cojin 43x43",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        1L,
                        new BigDecimal("1"),
                        new BigDecimal("43"),
                        new BigDecimal("43"),
                        "No aplica",
                        null))));

        assertThat(response.costoMateriales()).isEqualByComparingTo("616.3333");
    }

    @Test
    void fallaSiLaListaDeInsumosEstaVacia() {
        assertThatThrownBy(() -> productoService.crearProducto(new ProductoCreateRequest(
                        "COJ-PERS-001",
                        "Cojin 43x43",
                        ProductoTipo.COJIN_PERSONALIZADO,
                        List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Debes asociar al menos un insumo al producto.");
    }

    @Test
    void fallaSiElInsumoNoExiste() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-001", "Cojin 43x43");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);
        when(insumoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crearProducto(new ProductoCreateRequest(
                        "COJ-PERS-001",
                        "Cojin 43x43",
                        ProductoTipo.COJIN_PERSONALIZADO,
                        List.of(new ProductoInsumoCreateRequest(
                                999L,
                                new BigDecimal("1"),
                                null,
                                null,
                                "Pendiente",
                                null)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Insumo no encontrado: 999");
    }

    @Test
    void fallaSiLaCantidadUsadaEsCeroONegativa() {
        assertThatThrownBy(() -> productoService.crearProducto(new ProductoCreateRequest(
                        "COJ-PERS-001",
                        "Cojin 43x43",
                        ProductoTipo.COJIN_PERSONALIZADO,
                        List.of(new ProductoInsumoCreateRequest(
                                1L,
                                BigDecimal.ZERO,
                                null,
                                null,
                                "No aplica",
                                null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cantidad usada debe ser mayor a cero.");
    }

    @Test
    void guardaMedidasYConsumoEscaladosEnLaRelacion() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-001", "Cojin 43x43");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTextil, "1.2500", "43.120", "31.560", "Estimado");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(insumoTextil));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));

        productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-001",
                "Cojin 43x43",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        1L,
                        new BigDecimal("1.25"),
                        new BigDecimal("43.12"),
                        new BigDecimal("31.56"),
                        "Estimado",
                        new BigDecimal("500")))));

        ArgumentCaptor<ProductoInsumo> captor = ArgumentCaptor.forClass(ProductoInsumo.class);
        verify(productoInsumoRepository).save(captor.capture());
        assertThat(captor.getValue().getCantidadUsada()).isEqualByComparingTo("1.2500");
        assertThat(captor.getValue().getAnchoUsadoCm()).isEqualByComparingTo("43.120");
        assertThat(captor.getValue().getAltoLargoUsadoCm()).isEqualByComparingTo("31.560");
        assertThat(captor.getValue().getConsumo()).isEqualTo("Estimado");
    }

    private Producto productoPersistido(Long id, String codigo, String nombre) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setTipoProducto(ProductoTipo.COJIN_PERSONALIZADO);
        producto.setPrecioVenta(BigDecimal.ZERO);
        producto.setCostoMateriales(BigDecimal.ZERO);
        producto.setPrecioSugerido(BigDecimal.ZERO);
        producto.setGanancia(BigDecimal.ZERO);
        return producto;
    }

    private ProductoInsumo relacionPersistida(
            Long id,
            Producto producto,
            Insumo insumo,
            String cantidadUsada,
            String anchoUsadoCm,
            String altoLargoUsadoCm,
            String consumo) {
        ProductoInsumo productoInsumo = new ProductoInsumo();
        productoInsumo.setId(id);
        productoInsumo.setProducto(producto);
        productoInsumo.setInsumo(insumo);
        productoInsumo.setCantidadUsada(new BigDecimal(cantidadUsada));
        productoInsumo.setAnchoUsadoCm(anchoUsadoCm == null ? null : new BigDecimal(anchoUsadoCm));
        productoInsumo.setAltoLargoUsadoCm(altoLargoUsadoCm == null ? null : new BigDecimal(altoLargoUsadoCm));
        productoInsumo.setConsumo(consumo);
        return productoInsumo;
    }

    private InsumoCompra compraPersistida(Insumo insumo, String ancho, String alto, String precioCompraTotal) {
        InsumoCompra compra = new InsumoCompra();
        compra.setId(20L);
        compra.setInsumo(insumo);
        compra.setFechaCompra(LocalDate.of(2026, 7, 8));
        compra.setCantidadComprada(new BigDecimal("1.000"));
        compra.setUnidadMedida("rollo");
        compra.setAncho(new BigDecimal(ancho));
        compra.setAlto(new BigDecimal(alto));
        compra.setPrecioCompraTotal(new BigDecimal(precioCompraTotal));
        compra.setPrecioUnitario(new BigDecimal(precioCompraTotal));
        compra.setVigente(true);
        return compra;
    }
}
