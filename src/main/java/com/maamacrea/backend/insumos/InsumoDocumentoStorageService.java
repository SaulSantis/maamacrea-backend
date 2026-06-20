package com.maamacrea.backend.insumos;

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
public class InsumoDocumentoStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final Path storageDirectory;

    public InsumoDocumentoStorageService(
            @Value("${app.insumos.documentos.upload-dir:uploads/comprobantes-insumos}")
                    String uploadDir) {
        this.storageDirectory = Path.of(uploadDir).normalize();
    }

    public String guardarDocumento(Long insumoId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo valido para el comprobante.");
        }

        String originalFilename = file.getOriginalFilename();
        String safeOriginalName = originalFilename == null ? "documento" : Path.of(originalFilename).getFileName().toString();
        String extension = obtenerExtension(safeOriginalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("El comprobante debe estar en formato PDF, JPG, JPEG o PNG.");
        }

        String sanitizedBaseName = sanitizarNombre(removerExtension(safeOriginalName));
        if (sanitizedBaseName.isBlank()) {
            sanitizedBaseName = "documento";
        }

        String fileName =
                "insumo-"
                        + insumoId
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
                throw new IllegalArgumentException("No fue posible guardar el comprobante del insumo.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetFile.toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible guardar el comprobante del insumo.", exception);
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
