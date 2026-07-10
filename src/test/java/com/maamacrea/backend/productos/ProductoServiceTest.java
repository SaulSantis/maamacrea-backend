package com.maamacrea.backend.productos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
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
    private Insumo insumoPapelSublimacion;
    private Insumo insumoTinta;
    private Insumo insumoHilo;

    @BeforeEach
    void setUp() {
        insumoTextil = new Insumo();
        insumoTextil.setId(1L);
        insumoTextil.setCodigoProducto("TEL-BIS-001");
        insumoTextil.setNombre("Tela Bistrech Blanca");
        insumoTextil.setCategoria("Materiales textiles");
        insumoTextil.setUnidadMedida("rollo");
        insumoTextil.setCantidadComprada(new BigDecimal("1"));
        insumoTextil.setAncho(new BigDecimal("1.5"));
        insumoTextil.setUnidadAncho("m");
        insumoTextil.setAlto(new BigDecimal("50"));
        insumoTextil.setUnidadAlto("m");
        insumoTextil.setPrecioCompraTotal(new BigDecimal("40460"));
        insumoTextil.setCostoUnitario(new BigDecimal("40460.0000"));

        insumoSublimacion = new Insumo();
        insumoSublimacion.setId(3L);
        insumoSublimacion.setCodigoProducto("PAP-SUB-001");
        insumoSublimacion.setNombre("Papel sublimacion");
        insumoSublimacion.setCategoria("SUBLIMACION");
        insumoSublimacion.setUnidadMedida("unidad");
        insumoSublimacion.setCantidadComprada(new BigDecimal("50"));
        insumoSublimacion.setPrecioCompraTotal(new BigDecimal("10000"));
        insumoSublimacion.setCostoUnitario(new BigDecimal("200.0000"));

        insumoPapelSublimacion = new Insumo();
        insumoPapelSublimacion.setId(5L);
        insumoPapelSublimacion.setCodigoProducto("PP47170601");
        insumoPapelSublimacion.setNombre("Papel Subli S-Race Rollo T");
        insumoPapelSublimacion.setCategoria("SUBLIMACION");
        insumoPapelSublimacion.setUnidadMedida("rollo");
        insumoPapelSublimacion.setCantidadComprada(new BigDecimal("1"));
        insumoPapelSublimacion.setAncho(new BigDecimal("61"));
        insumoPapelSublimacion.setUnidadAncho("cm");
        insumoPapelSublimacion.setAlto(new BigDecimal("55"));
        insumoPapelSublimacion.setUnidadAlto("m");
        insumoPapelSublimacion.setPrecioCompraTotal(new BigDecimal("35990"));
        insumoPapelSublimacion.setCostoUnitario(new BigDecimal("35990.0000"));

        insumoTinta = new Insumo();
        insumoTinta.setId(4L);
        insumoTinta.setCodigoProducto("T49M1");
        insumoTinta.setNombre("Tinta Negra Sublimacion Epson");
        insumoTinta.setCategoria("SUBLIMACION");
        insumoTinta.setUnidadMedida("ml");
        insumoTinta.setCantidadComprada(new BigDecimal("1"));
        insumoTinta.setCantidadMlComprados(new BigDecimal("140"));
        insumoTinta.setPrecioCompraTotal(new BigDecimal("19990"));
        insumoTinta.setCostoUnitario(new BigDecimal("142.7857"));

        insumoHilo = new Insumo();
        insumoHilo.setId(6L);
        insumoHilo.setCodigoProducto("CON-HIL-BLA-001");
        insumoHilo.setNombre("Cono Hilo Blanco");
        insumoHilo.setCategoria("COSTURA_Y_CONFECCION");
        insumoHilo.setUnidadMedida("cono");
        insumoHilo.setCantidadComprada(new BigDecimal("9"));
        insumoHilo.setContenidoPorUnidad(new BigDecimal("1829"));
        insumoHilo.setUnidadContenido("m");
        insumoHilo.setContenidoTotalComprado(new BigDecimal("16461"));
        insumoHilo.setPrecioCompraTotal(new BigDecimal("18000"));
        insumoHilo.setCostoUnitario(new BigDecimal("1.0935"));
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
        InsumoCompra compraVigente = compraPersistida(insumoTextil, "1.500", "m", "50.000", "m", "40460.00");

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

        assertThat(response.costoMateriales()).isEqualByComparingTo("99.7474");
        assertThat(response.precioSugerido()).isEqualByComparingTo("199.49");
        assertThat(response.ganancia()).isEqualByComparingTo("99.74");
        assertThat(response.costeoCompleto()).isTrue();
        assertThat(response.advertenciasCosteo()).isEmpty();
        assertThat(response.insumos().get(0).costoEstimado()).isEqualByComparingTo("99.7474");
        assertThat(response.insumos().get(0).mensajeCosto()).isNull();
    }

    @Test
    void calculaCostoTextilConCantidadDosYOtraMedida() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-002", "Cojin 31x43");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTextil, "2.0000", "31.000", "43.000", "No aplica");
        InsumoCompra compraVigente = compraPersistida(insumoTextil, "1.500", "m", "50.000", "m", "40460.00");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-002")).thenReturn(false);
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
                "COJ-PERS-002",
                "Cojin 31x43",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        1L,
                        new BigDecimal("2"),
                        new BigDecimal("31"),
                        new BigDecimal("43"),
                        "No aplica",
                        null))));

        assertThat(response.costoMateriales()).isEqualByComparingTo("143.8218");
        assertThat(response.insumos().get(0).costoEstimado()).isEqualByComparingTo("143.8218");
    }

    @Test
    void calculaCostoDeHiloPorMetroCuandoElInsumoEsCono() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-003", "Cojin 40x40");
        ProductoInsumo relacionGuardada = relacionPersistida(
                13L, productoGuardado, insumoHilo, "3.0000", null, null, "3 m");

        InsumoCompra compraVigente = new InsumoCompra();
        compraVigente.setId(21L);
        compraVigente.setInsumo(insumoHilo);
        compraVigente.setFechaCompra(LocalDate.of(2026, 7, 9));
        compraVigente.setCantidadComprada(new BigDecimal("9.000"));
        compraVigente.setUnidadMedida("cono");
        compraVigente.setContenidoPorUnidad(new BigDecimal("1829.0000"));
        compraVigente.setUnidadContenido("m");
        compraVigente.setContenidoTotalComprado(new BigDecimal("16461.0000"));
        compraVigente.setPrecioCompraTotal(new BigDecimal("18000.00"));
        compraVigente.setPrecioUnitario(new BigDecimal("1.0935"));
        compraVigente.setVigente(true);

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-003")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(1L);
            }
            return producto;
        });
        when(insumoRepository.findById(6L)).thenReturn(Optional.of(insumoHilo));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(6L))
                .thenReturn(Optional.of(compraVigente));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-003",
                "Cojin 40x40",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        6L,
                        new BigDecimal("3"),
                        null,
                        null,
                        "3 m",
                        null))));

        assertThat(response.costeoCompleto()).isTrue();
        assertThat(response.advertenciasCosteo()).isEmpty();
        assertThat(response.costoMateriales()).isEqualByComparingTo("3.2805");
        assertThat(response.insumos().get(0).costoEstimado()).isEqualByComparingTo("3.2805");
        assertThat(response.insumos().get(0).medidaUsadaTexto()).isEqualTo("No aplica");
    }

    @Test
    void informaAdvertenciaCuandoFaltanMedidasDeCompraParaCosteoPorArea() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-003", "Cojin sin largo");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTextil, "1.0000", "43.000", "43.000", "No aplica");
        InsumoCompra compraVigente = compraPersistida(insumoTextil, "1.500", "m", null, null, "40460.00");
        insumoTextil.setAlto(null);

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-003")).thenReturn(false);
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
                "COJ-PERS-003",
                "Cojin sin largo",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        1L,
                        new BigDecimal("1"),
                        new BigDecimal("43"),
                        new BigDecimal("43"),
                        "No aplica",
                        null))));

        assertThat(response.costeoCompleto()).isFalse();
        assertThat(response.advertenciasCosteo())
                .contains("Falta largo de compra para calcular costo por area en Tela Bistrech Blanca.");
        assertThat(response.insumos().get(0).costoEstimado()).isNull();
        assertThat(response.insumos().get(0).mensajeCosto())
                .isEqualTo("Falta largo de compra para calcular costo por area en Tela Bistrech Blanca.");
    }

    @Test
    void calculaCostoPapelSublimacionPorAreaSinClasificarloComoTinta() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PER-001", "Cojin Personalizado 40x40");
        ProductoInsumo primerCorte = relacionPersistida(
                10L, productoGuardado, insumoPapelSublimacion, "1.0000", "42.000", "42.000", "No aplica");
        ProductoInsumo segundoCorte = relacionPersistida(
                11L, productoGuardado, insumoPapelSublimacion, "1.0000", "16.000", "8.000", "No aplica");
        InsumoCompra compraVigente =
                compraPersistida(insumoPapelSublimacion, "61.000", "cm", "55.000", "m", "35990.00");

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PER-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(1L);
            }
            return producto;
        });
        when(insumoRepository.findById(5L)).thenReturn(Optional.of(insumoPapelSublimacion));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(primerCorte, segundoCorte);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L))
                .thenReturn(List.of(primerCorte, segundoCorte));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(5L))
                .thenReturn(Optional.of(compraVigente));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PER-001",
                "Cojin Personalizado 40x40",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(
                        new ProductoInsumoCreateRequest(
                                5L,
                                new BigDecimal("1"),
                                new BigDecimal("42"),
                                new BigDecimal("42"),
                                "No aplica",
                                null),
                        new ProductoInsumoCreateRequest(
                                5L,
                                new BigDecimal("1"),
                                new BigDecimal("16"),
                                new BigDecimal("8"),
                                "No aplica",
                                null))));

        assertThat(response.costeoCompleto()).isTrue();
        assertThat(response.advertenciasCosteo()).isEmpty();
        assertThat(response.insumos()).hasSize(2);
        assertThat(response.insumos().get(0).costoEstimado())
                .isCloseTo(new BigDecimal("189.2"), within(new BigDecimal("0.5")));
        assertThat(response.insumos().get(1).costoEstimado())
                .isCloseTo(new BigDecimal("13.7"), within(new BigDecimal("0.5")));
        assertThat(response.insumos().get(0).mensajeCosto()).isNull();
        assertThat(response.insumos().get(1).mensajeCosto()).isNull();
    }

    @Test
    void calculaCostoTintaUsandoCantidadMlCompradosYCantidadUsadaManual() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-004", "Cojin sublimado");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTinta, "0.8825", null, null, "0.8825 ML");
        InsumoCompra compraVigente = compraPersistida(insumoTinta, null, null, null, null, "19990.00");
        compraVigente.setUnidadMedida("ml");
        compraVigente.setCantidadComprada(new BigDecimal("1.000"));
        compraVigente.setCantidadMlComprados(new BigDecimal("140.0000"));
        compraVigente.setPrecioUnitario(new BigDecimal("142.7857"));

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-004")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(1L);
            }
            return producto;
        });
        when(insumoRepository.findById(4L)).thenReturn(Optional.of(insumoTinta));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(4L))
                .thenReturn(Optional.of(compraVigente));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-004",
                "Cojin sublimado",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        4L,
                        new BigDecimal("0.8825"),
                        null,
                        null,
                        "0.8825 ML",
                        null))));

        assertThat(response.costoMateriales()).isEqualByComparingTo("126.0084");
        assertThat(response.insumos().get(0).costoEstimado()).isEqualByComparingTo("126.0084");
    }

    @Test
    void informaAdvertenciaCuandoTintaNoTieneCantidadMlComprados() {
        Producto productoGuardado = productoPersistido(1L, "COJ-PERS-005", "Cojin sublimado");
        ProductoInsumo relacionGuardada = relacionPersistida(
                10L, productoGuardado, insumoTinta, "0.8825", null, null, "0.8825 ML");
        InsumoCompra compraVigente = compraPersistida(insumoTinta, null, null, null, null, "19990.00");
        compraVigente.setUnidadMedida("ml");
        compraVigente.setCantidadComprada(new BigDecimal("1.000"));
        compraVigente.setCantidadMlComprados(null);
        compraVigente.setPrecioUnitario(new BigDecimal("19990.0000"));
        insumoTinta.setCantidadMlComprados(null);

        when(productoRepository.existsByCodigoIgnoreCase("COJ-PERS-005")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto producto = invocation.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(1L);
            }
            return producto;
        });
        when(insumoRepository.findById(4L)).thenReturn(Optional.of(insumoTinta));
        when(productoInsumoRepository.save(any(ProductoInsumo.class))).thenReturn(relacionGuardada);
        when(productoInsumoRepository.findByProductoIdOrderByIdAsc(1L)).thenReturn(List.of(relacionGuardada));
        when(insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(4L))
                .thenReturn(Optional.of(compraVigente));

        ProductoResponse response = productoService.crearProducto(new ProductoCreateRequest(
                "COJ-PERS-005",
                "Cojin sublimado",
                ProductoTipo.COJIN_PERSONALIZADO,
                List.of(new ProductoInsumoCreateRequest(
                        4L,
                        new BigDecimal("0.8825"),
                        null,
                        null,
                        "0.8825 ML",
                        null))));

        assertThat(response.costeoCompleto()).isFalse();
        assertThat(response.advertenciasCosteo()).contains("Faltan ML comprados para calcular tinta en Tinta Negra Sublimacion Epson.");
        assertThat(response.insumos().get(0).costoEstimado()).isNull();
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

    private InsumoCompra compraPersistida(
            Insumo insumo,
            String ancho,
            String unidadAncho,
            String alto,
            String unidadAlto,
            String precioCompraTotal) {
        InsumoCompra compra = new InsumoCompra();
        compra.setId(20L);
        compra.setInsumo(insumo);
        compra.setFechaCompra(LocalDate.of(2026, 7, 8));
        compra.setCantidadComprada(new BigDecimal("1.000"));
        compra.setUnidadMedida("rollo");
        compra.setAncho(ancho == null ? null : new BigDecimal(ancho));
        compra.setUnidadAncho(unidadAncho);
        compra.setAlto(alto == null ? null : new BigDecimal(alto));
        compra.setUnidadAlto(unidadAlto);
        compra.setPrecioCompraTotal(new BigDecimal(precioCompraTotal));
        compra.setPrecioUnitario(new BigDecimal(precioCompraTotal));
        compra.setVigente(true);
        return compra;
    }
}
