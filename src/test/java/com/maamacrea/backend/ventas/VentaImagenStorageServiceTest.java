package com.maamacrea.backend.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maamacrea.backend.ApiRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class VentaImagenStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void guardaImagenOptimizadaYMiniaturaDentroDeLaCarpetaDeLaVenta() throws Exception {
        VentaImagenStorageService storageService = buildStorageService();

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.png", "image/png", createValidPngBytes());

        VentaImagenStorageService.StoredVentaUpload storedUpload = storageService.guardarArchivo(15L, file);
        Path storedImage = tempDir.resolve(storedUpload.storedImagePath()).normalize();
        Path storedThumbnail = tempDir.resolve(storedUpload.storedThumbnailPath()).normalize();

        assertThat(storedUpload.storedImagePath()).startsWith("uploads/ventas/15/images/").endsWith(".png");
        assertThat(storedUpload.storedThumbnailPath()).startsWith("uploads/ventas/15/thumbnails/").contains("-thumb");
        assertThat(Files.exists(storedImage)).isTrue();
        assertThat(Files.exists(storedThumbnail)).isTrue();
        assertThat(storageService.cargarImagen(storedUpload.storedImagePath()).fileName())
                .isEqualTo(storedImage.getFileName().toString());
        assertThat(storageService.cargarMiniatura(storedUpload.storedThumbnailPath(), storedUpload.storedImagePath()).fileName())
                .isEqualTo(storedThumbnail.getFileName().toString());
    }

    @Test
    void rechazaExtensionNoPermitida() throws Exception {
        VentaImagenStorageService storageService = buildStorageService();

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.txt", "text/plain", createValidPngBytes());

        assertThatThrownBy(() -> storageService.guardarArchivo(16L, file))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("El formato del archivo no es compatible.");
    }

    @Test
    void rechazaImagenInvalidaAunqueTengaExtensionPermitida() {
        VentaImagenStorageService storageService = buildStorageService();

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.png", "image/png", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storageService.guardarArchivo(17L, file))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage("El archivo seleccionado no contiene una imagen valida.");
    }

    @Test
    void eliminaImagenYMiniaturaGuardadas() throws Exception {
        VentaImagenStorageService storageService = buildStorageService();

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.png", "image/png", createValidPngBytes());

        VentaImagenStorageService.StoredVentaUpload storedUpload = storageService.guardarArchivo(18L, file);
        Path storedImage = tempDir.resolve(storedUpload.storedImagePath()).normalize();
        Path storedThumbnail = tempDir.resolve(storedUpload.storedThumbnailPath()).normalize();

        storageService.eliminarImagen(storedUpload.storedImagePath());
        storageService.eliminarImagen(storedUpload.storedThumbnailPath());

        assertThat(Files.exists(storedImage)).isFalse();
        assertThat(Files.exists(storedThumbnail)).isFalse();
    }

    private VentaImagenStorageService buildStorageService() {
        return new VentaImagenStorageService(
                tempDir.resolve("uploads/ventas").toString(),
                30,
                2,
                1600,
                400,
                0.82f,
                40_000_000L);
    }

    private byte[] createValidPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }
}
