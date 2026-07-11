package com.maamacrea.backend.ventas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VentaImagenStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    private final Path storageDirectory;

    public VentaImagenStorageService(
            @Value("${app.ventas.imagenes.upload-dir:uploads/imagenes-ventas}")
                    String uploadDir) {
        this.storageDirectory = Path.of(uploadDir).normalize();
    }

    public String guardarImagen(Long ventaId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo valido para el diseño vendido.");
        }

        String originalFilename = file.getOriginalFilename();
        String safeOriginalName =
                originalFilename == null ? "archivo-diseno" : Path.of(originalFilename).getFileName().toString();
        String extension = obtenerExtension(safeOriginalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "El archivo del diseño debe estar en formato PNG, JPG, JPEG, WEBP o PDF.");
        }

        String sanitizedBaseName = sanitizarNombre(removerExtension(safeOriginalName));
        if (sanitizedBaseName.isBlank()) {
            sanitizedBaseName = "archivo-diseno";
        }

        String fileName =
                "venta-"
                        + ventaId
                        + "-"
                        + System.currentTimeMillis()
                        + "-"
                        + sanitizedBaseName
                        + "."
                        + extension;

        try {
            Files.createDirectories(storageDirectory);
            Path targetFile = storageDirectory.resolve(fileName).normalize();
            if (!targetFile.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("No fue posible guardar el archivo del diseño vendido.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetFile.toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible guardar el archivo del diseño vendido.", exception);
        }
    }

    private String obtenerExtension(String fileName) {
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex < 0 || extensionSeparatorIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionSeparatorIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String removerExtension(String fileName) {
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex < 0) {
            return fileName;
        }

        return fileName.substring(0, extensionSeparatorIndex);
    }

    private String sanitizarNombre(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
    }
}
