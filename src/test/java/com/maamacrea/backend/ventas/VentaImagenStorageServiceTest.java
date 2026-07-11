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
    void guardaArchivoPdfYDevuelveRutaRelativaDentroDeUploads() throws Exception {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas/disenos").toString());

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.pdf", "application/pdf", new byte[] {1, 2, 3});

        String storedPath = storageService.guardarImagen(15L, "COJ-PER-001", file);
        Path storedFile = tempDir.resolve(storedPath).normalize();

        assertThat(storedPath).startsWith("uploads/ventas/disenos/").endsWith(".pdf");
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storageService.cargarImagen(storedPath).fileName()).isEqualTo(storedFile.getFileName().toString());
    }

    @Test
    void rechazaExtensionNoPermitida() {
        VentaImagenStorageService storageService =
                new VentaImagenStorageService(tempDir.resolve("uploads/ventas/disenos").toString());

        MockMultipartFile file =
                new MockMultipartFile("file", "COJ-PER-001.txt", "text/plain", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storageService.guardarImagen(16L, "COJ-PER-001", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo del diseno debe ser PNG, JPG, JPEG, WEBP o PDF.");
    }
}
