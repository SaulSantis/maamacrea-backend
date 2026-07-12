package com.maamacrea.backend.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class VentaImagenStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void guardaArchivoPngDentroDeLaCarpetaDeLaVentaYDevuelveRutaRelativa() throws Exception {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas").toString(), 10);

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.png", "image/png", new byte[] {1, 2, 3});

        VentaImagenStorageService.StoredVentaUpload storedUpload = storageService.guardarArchivo(15L, file);
        Path storedFile = tempDir.resolve(storedUpload.storedPath()).normalize();

        assertThat(storedUpload.storedPath()).startsWith("uploads/ventas/15/").endsWith(".png");
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storageService.cargarImagen(storedUpload.storedPath()).fileName())
                .isEqualTo(storedFile.getFileName().toString());
    }

    @Test
    void rechazaExtensionNoPermitida() {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas").toString(), 10);

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.txt", "text/plain", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storageService.guardarArchivo(16L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El formato del archivo no es compatible.");
    }

    @Test
    void rechazaArchivoConMimeInvalidoAunqueTengaExtensionPermitida() {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas").toString(), 10);

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.png", "text/plain", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storageService.guardarArchivo(17L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El formato del archivo no es compatible.");
    }

    @Test
    void eliminaArchivoGuardadoCuandoLaVentaSeBorra() throws Exception {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas").toString(), 10);

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.pdf", "application/pdf", new byte[] {1, 2, 3});

        String storedPath = storageService.guardarArchivo(18L, file).storedPath();
        Path storedFile = tempDir.resolve(storedPath).normalize();

        storageService.eliminarImagen(storedPath);

        assertThat(Files.exists(storedFile)).isFalse();
    }
}
