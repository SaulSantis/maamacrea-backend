package com.maamacrea.backend.ventas;

import com.maamacrea.backend.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VentaImagenStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Logger LOGGER = LoggerFactory.getLogger(VentaImagenStorageService.class);

    private final Path storageDirectory;
    private final Path uploadsRoot;

    public VentaImagenStorageService(
            @Value("${app.ventas.imagenes.upload-dir:uploads/ventas/disenos}")
                    String uploadDir) {
        this.storageDirectory = Path.of(uploadDir).normalize();
        this.uploadsRoot = Path.of("uploads").toAbsolutePath().normalize();
    }

    public String guardarImagen(Long ventaId, String codigoVendido, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar un archivo valido para el diseno vendido.");
        }

        String safeOriginalName = obtenerNombreSeguro(file.getOriginalFilename(), "archivo-diseno");
        String extension = obtenerExtension(safeOriginalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "El archivo del diseno debe ser PNG, JPG, JPEG, WEBP o PDF.");
        }

        String sanitizedBaseName = sanitizarNombre(codigoVendido);
        if (sanitizedBaseName.isBlank()) {
            sanitizedBaseName = "VENTA-" + ventaId;
        }

        String fileName =
                sanitizedBaseName
                        + "-"
                        + LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER)
                        + "."
                        + extension;

        try {
            Files.createDirectories(storageDirectory);
            Path targetFile = storageDirectory.resolve(fileName).normalize();
            if (!targetFile.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("No fue posible guardar el archivo del diseno vendido.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            LOGGER.info(
                    "Archivo de diseno guardado para venta {}. original='{}', final='{}'",
                    ventaId,
                    safeOriginalName,
                    targetFile);

            return targetFile.toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible guardar el archivo del diseno vendido.", exception);
        }
    }

    public StoredVentaDesignFile cargarImagen(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new ResourceNotFoundException("La venta no tiene archivo del diseno adjunto.");
        }

        try {
            Path normalizedStoredPath = Path.of(storedPath).normalize();
            Path absoluteFilePath = normalizedStoredPath.isAbsolute()
                    ? normalizedStoredPath
                    : Path.of("").toAbsolutePath().resolve(normalizedStoredPath).normalize();

            if (!absoluteFilePath.startsWith(uploadsRoot)) {
                throw new IllegalArgumentException("No fue posible acceder al archivo del diseno vendido.");
            }

            if (!Files.exists(absoluteFilePath) || !Files.isRegularFile(absoluteFilePath)) {
                throw new ResourceNotFoundException("No existe el archivo del diseno solicitado.");
            }

            Resource resource = new UrlResource(absoluteFilePath.toUri());
            String contentType = Files.probeContentType(absoluteFilePath);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);

            return new StoredVentaDesignFile(resource, mediaType, absoluteFilePath.getFileName().toString());
        } catch (InvalidPathException | IOException exception) {
            throw new IllegalStateException("No fue posible acceder al archivo del diseno vendido.", exception);
        }
    }

    private String obtenerExtension(String fileName) {
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex < 0 || extensionSeparatorIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionSeparatorIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizarNombre(String fileName) {
        String normalizedValue = fileName == null ? "" : Normalizer.normalize(fileName, Normalizer.Form.NFD);
        return normalizedValue
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9_-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "")
                .toUpperCase(Locale.ROOT);
    }

    private String obtenerNombreSeguro(String originalFilename, String fallbackName) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallbackName;
        }

        String normalizedName = originalFilename.replace('\\', '/');
        int lastSeparatorIndex = normalizedName.lastIndexOf('/');
        return lastSeparatorIndex >= 0 ? normalizedName.substring(lastSeparatorIndex + 1) : normalizedName;
    }

    public record StoredVentaDesignFile(Resource resource, MediaType mediaType, String fileName) {}
}
