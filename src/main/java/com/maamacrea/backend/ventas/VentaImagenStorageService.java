package com.maamacrea.backend.ventas;

import com.maamacrea.backend.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(VentaImagenStorageService.class);
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES_BY_EXTENSION = Map.of(
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg", "image/jpg"),
            "jpeg", Set.of("image/jpeg", "image/jpg"),
            "webp", Set.of("image/webp"),
            "pdf", Set.of("application/pdf"));

    private final Path storageDirectory;
    private final Path uploadsRoot;
    private final Path applicationRoot;
    private final long maxFileSizeBytes;

    public VentaImagenStorageService(
            @Value("${app.ventas.archivos.upload-dir:${app.ventas.imagenes.upload-dir:uploads/ventas}}")
                    String uploadDir,
            @Value("${app.ventas.archivos.max-size-mb:10}")
                    long maxFileSizeMb) {
        this.applicationRoot = resolverRaizAplicacion();
        Path configuredDirectory = Path.of(uploadDir).normalize();
        this.storageDirectory = configuredDirectory.isAbsolute()
                ? configuredDirectory
                : applicationRoot.resolve(configuredDirectory).normalize();
        this.uploadsRoot = resolverUploadsRoot(configuredDirectory);
        this.maxFileSizeBytes = Math.max(1L, maxFileSizeMb) * 1024L * 1024L;
    }

    public StoredVentaUpload guardarArchivo(Long ventaId, MultipartFile file) {
        validarArchivo(file);

        String originalFileName = obtenerNombreSeguro(file.getOriginalFilename(), "archivo-diseno");
        String extension = obtenerExtension(originalFileName);
        MediaType mediaType = resolverMediaType(extension, file.getContentType());
        Path saleDirectory = storageDirectory.resolve(String.valueOf(ventaId)).normalize();

        try {
            Files.createDirectories(saleDirectory);
            if (!Files.isWritable(saleDirectory)) {
                throw new IllegalStateException("No fue posible escribir en el directorio de archivos de ventas.");
            }

            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetFile = saleDirectory.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(saleDirectory)) {
                throw new IllegalArgumentException("No fue posible guardar el archivo del diseno vendido.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                Files.deleteIfExists(targetFile);
                throw exception;
            }

            LOGGER.info(
                    "Archivo de diseno guardado para venta {}. original='{}', final='{}'",
                    ventaId,
                    originalFileName,
                    targetFile);

            return new StoredVentaUpload(
                    originalFileName,
                    storedFileName,
                    construirRutaRelativa(targetFile),
                    mediaType.toString(),
                    file.getSize());
        } catch (IOException exception) {
            LOGGER.error("No fue posible guardar el archivo de diseno para la venta {}", ventaId, exception);
            throw new IllegalStateException("No fue posible guardar el archivo del diseno vendido.", exception);
        }
    }

    public String guardarImagen(Long ventaId, String codigoVendido, MultipartFile file) {
        return guardarArchivo(ventaId, file).storedPath();
    }

    public StoredVentaDesignFile cargarImagen(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new ResourceNotFoundException("La venta no tiene archivos del diseno adjuntos.");
        }

        try {
            Path normalizedStoredPath = Path.of(storedPath.replace('\\', '/')).normalize();
            Path absoluteFilePath = normalizedStoredPath.isAbsolute()
                    ? normalizedStoredPath
                    : resolverRutaAbsoluta(normalizedStoredPath);

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
            LOGGER.error("No fue posible acceder al archivo almacenado '{}'", storedPath, exception);
            throw new IllegalStateException("No fue posible acceder al archivo del diseno vendido.", exception);
        }
    }

    public void eliminarImagen(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        try {
            Path normalizedStoredPath = Path.of(storedPath.replace('\\', '/')).normalize();
            Path absoluteFilePath = normalizedStoredPath.isAbsolute()
                    ? normalizedStoredPath
                    : resolverRutaAbsoluta(normalizedStoredPath);

            if (!absoluteFilePath.startsWith(uploadsRoot)) {
                throw new IllegalArgumentException("No fue posible eliminar el archivo del diseno vendido.");
            }

            if (Files.deleteIfExists(absoluteFilePath)) {
                LOGGER.info("Archivo de diseno eliminado: {}", absoluteFilePath);
            }
        } catch (InvalidPathException | IOException exception) {
            LOGGER.error("No fue posible eliminar el archivo almacenado '{}'", storedPath, exception);
            throw new IllegalStateException("No fue posible eliminar el archivo del diseno vendido.", exception);
        }
    }

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo seleccionado esta vacio.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("El archivo supera el tamano permitido.");
        }

        String originalFileName = obtenerNombreSeguro(file.getOriginalFilename(), "archivo-diseno");
        String extension = obtenerExtension(originalFileName);
        if (!ALLOWED_MIME_TYPES_BY_EXTENSION.containsKey(extension)) {
            throw new IllegalArgumentException("El formato del archivo no es compatible.");
        }

        resolverMediaType(extension, file.getContentType());
    }

    private MediaType resolverMediaType(String extension, String rawContentType) {
        Set<String> allowedContentTypes = ALLOWED_MIME_TYPES_BY_EXTENSION.get(extension);
        if (allowedContentTypes == null) {
            throw new IllegalArgumentException("El formato del archivo no es compatible.");
        }

        String normalizedContentType = normalizarContentType(rawContentType);
        if (normalizedContentType == null || !allowedContentTypes.contains(normalizedContentType)) {
            throw new IllegalArgumentException("El formato del archivo no es compatible.");
        }

        return MediaType.parseMediaType(normalizedContentType);
    }

    private String normalizarContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }

        return normalized.isBlank() ? null : normalized;
    }

    private String construirRutaRelativa(Path targetFile) {
        if (targetFile.startsWith(uploadsRoot)) {
            Path relativePath = uploadsRoot.relativize(targetFile);
            return "uploads/" + relativePath.toString().replace('\\', '/');
        }

        return targetFile.getFileName().toString();
    }

    private Path resolverUploadsRoot(Path configuredDirectory) {
        if (!configuredDirectory.isAbsolute()) {
            return applicationRoot.resolve("uploads").normalize();
        }

        for (int index = 0; index < storageDirectory.getNameCount(); index++) {
            if (!"uploads".equalsIgnoreCase(storageDirectory.getName(index).toString())) {
                continue;
            }

            Path uploadsPath = storageDirectory.getRoot();
            if (uploadsPath == null) {
                uploadsPath = Path.of("");
            }

            for (int nameIndex = 0; nameIndex <= index; nameIndex++) {
                uploadsPath = uploadsPath.resolve(storageDirectory.getName(nameIndex).toString());
            }

            return uploadsPath.normalize();
        }

        Path parentDirectory = storageDirectory.getParent();
        return parentDirectory == null ? storageDirectory : parentDirectory.normalize();
    }

    private Path resolverRutaAbsoluta(Path storedPath) {
        if (storedPath.getNameCount() == 0) {
            return storageDirectory;
        }

        if ("uploads".equalsIgnoreCase(storedPath.getName(0).toString())) {
            Path relativePathInsideUploads = storedPath.getNameCount() == 1
                    ? Path.of("")
                    : storedPath.subpath(1, storedPath.getNameCount());
            return uploadsRoot.resolve(relativePathInsideUploads).normalize();
        }

        return storageDirectory.resolve(storedPath).normalize();
    }

    private Path resolverRaizAplicacion() {
        List<Path> candidateRoots = List.of(
                Path.of("").toAbsolutePath().normalize(),
                obtenerRaizDesdeCodeSource());

        for (Path candidateRoot : candidateRoots) {
            Path projectRoot = buscarRaizDeProyecto(candidateRoot);
            if (projectRoot != null) {
                return projectRoot.normalize();
            }
        }

        return Path.of("").toAbsolutePath().normalize();
    }

    private Path obtenerRaizDesdeCodeSource() {
        try {
            Path codeSourcePath = Path.of(
                            VentaImagenStorageService.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI())
                    .normalize();
            return Files.isRegularFile(codeSourcePath)
                    ? codeSourcePath.getParent()
                    : codeSourcePath;
        } catch (URISyntaxException | IllegalStateException exception) {
            return Path.of("").toAbsolutePath().normalize();
        }
    }

    private Path buscarRaizDeProyecto(Path candidateRoot) {
        Path currentPath = candidateRoot;

        while (currentPath != null) {
            if (Files.exists(currentPath.resolve("pom.xml"))) {
                return currentPath;
            }

            currentPath = currentPath.getParent();
        }

        return null;
    }

    private String obtenerExtension(String fileName) {
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex < 0 || extensionSeparatorIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionSeparatorIndex + 1).toLowerCase(Locale.ROOT);
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

    public record StoredVentaUpload(
            String originalFileName,
            String storedFileName,
            String storedPath,
            String mediaType,
            long sizeBytes) {}
}
