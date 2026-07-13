package com.maamacrea.backend.ventas;

import com.maamacrea.backend.ApiRequestException;
import com.maamacrea.backend.ResourceNotFoundException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VentaImagenStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VentaImagenStorageService.class);
    private static final Set<String> ALLOWED_INPUT_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> ALLOWED_INPUT_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    private final Path storageDirectory;
    private final Path uploadsRoot;
    private final Path applicationRoot;
    private final long maxOriginalFileSizeBytes;
    private final long maxOptimizedFileSizeBytes;
    private final int maxDimension;
    private final int thumbnailDimension;
    private final float quality;
    private final long maxPixelCount;

    public VentaImagenStorageService(
            @Value("${app.ventas.archivos.upload-dir:${app.ventas.imagenes.upload-dir:uploads/ventas}}")
                    String uploadDir,
            @Value("${app.ventas.images.max-original-size-mb:30}")
                    long maxOriginalFileSizeMb,
            @Value("${app.ventas.images.max-optimized-size-mb:2}")
                    long maxOptimizedFileSizeMb,
            @Value("${app.ventas.images.max-dimension:1600}")
                    int maxDimension,
            @Value("${app.ventas.images.thumbnail-dimension:400}")
                    int thumbnailDimension,
            @Value("${app.ventas.images.quality:0.82}")
                    float quality,
            @Value("${app.ventas.images.max-pixels:40000000}")
                    long maxPixelCount) {
        this.applicationRoot = resolverRaizAplicacion();
        Path configuredDirectory = Path.of(uploadDir).normalize();
        this.storageDirectory = configuredDirectory.isAbsolute()
                ? configuredDirectory
                : applicationRoot.resolve(configuredDirectory).normalize();
        this.uploadsRoot = resolverUploadsRoot(configuredDirectory);
        this.maxOriginalFileSizeBytes = Math.max(1L, maxOriginalFileSizeMb) * 1024L * 1024L;
        this.maxOptimizedFileSizeBytes = Math.max(1L, maxOptimizedFileSizeMb) * 1024L * 1024L;
        this.maxDimension = Math.max(320, maxDimension);
        this.thumbnailDimension = Math.max(120, thumbnailDimension);
        this.quality = clampQuality(quality);
        this.maxPixelCount = Math.max(1L, maxPixelCount);
    }

    public StoredVentaUpload guardarArchivo(Long ventaId, MultipartFile file) {
        validateMultipartFile(file);

        String originalFileName = obtenerNombreSeguro(file.getOriginalFilename(), "imagen-venta");
        String extension = obtenerExtension(originalFileName);
        String normalizedContentType = normalizarContentType(file.getContentType());

        if (!ALLOWED_INPUT_EXTENSIONS.contains(extension) || !ALLOWED_INPUT_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    "FORMATO_NO_COMPATIBLE",
                    "El formato del archivo no es compatible.");
        }

        try {
            byte[] originalBytes = file.getBytes();
            validateOriginalSize(originalBytes.length);

            BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (decodedImage == null) {
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "IMAGEN_INVALIDA",
                        "El archivo seleccionado no contiene una imagen valida.");
            }

            int exifOrientation = resolveExifOrientation(originalBytes);
            BufferedImage orientedImage = applyOrientation(decodedImage, exifOrientation);
            validatePixelCount(orientedImage);

            int originalWidth = orientedImage.getWidth();
            int originalHeight = orientedImage.getHeight();

            BufferedImage resizedImage = resizeProportionally(orientedImage, maxDimension);
            boolean hasTransparency = hasTransparency(resizedImage);
            OutputFormat outputFormat = hasTransparency ? OutputFormat.PNG : OutputFormat.JPEG;

            EncodedImage optimizedImage = encodeOptimizedImage(resizedImage, outputFormat);
            BufferedImage thumbnailBaseImage = resizeProportionally(resizedImage, thumbnailDimension);
            EncodedImage thumbnailImage = encodeThumbnailImage(thumbnailBaseImage, outputFormat);

            Path saleImagesDirectory = storageDirectory.resolve(String.valueOf(ventaId)).resolve("images").normalize();
            Path saleThumbnailsDirectory = storageDirectory.resolve(String.valueOf(ventaId)).resolve("thumbnails").normalize();
            Files.createDirectories(saleImagesDirectory);
            Files.createDirectories(saleThumbnailsDirectory);
            validateWritableDirectory(saleImagesDirectory);
            validateWritableDirectory(saleThumbnailsDirectory);

            String baseFileName = UUID.randomUUID().toString();
            String storedFileName = baseFileName + outputFormat.extension();
            String thumbnailFileName = baseFileName + "-thumb" + outputFormat.extension();
            Path optimizedTargetFile = saleImagesDirectory.resolve(storedFileName).normalize();
            Path thumbnailTargetFile = saleThumbnailsDirectory.resolve(thumbnailFileName).normalize();
            validateTargetPath(optimizedTargetFile, saleImagesDirectory);
            validateTargetPath(thumbnailTargetFile, saleThumbnailsDirectory);

            writeBytesAtomically(optimizedTargetFile, optimizedImage.bytes());
            try {
                writeBytesAtomically(thumbnailTargetFile, thumbnailImage.bytes());
            } catch (RuntimeException exception) {
                Files.deleteIfExists(optimizedTargetFile);
                throw exception;
            }

            LOGGER.info(
                    "Imagen optimizada de venta guardada para venta {}. original='{}', optimizada='{}', miniatura='{}'",
                    ventaId,
                    originalFileName,
                    optimizedTargetFile,
                    thumbnailTargetFile);

            return new StoredVentaUpload(
                    originalFileName,
                    storedFileName,
                    thumbnailFileName,
                    construirRutaRelativa(optimizedTargetFile),
                    construirRutaRelativa(thumbnailTargetFile),
                    outputFormat.mediaType(),
                    outputFormat.label(),
                    file.getSize(),
                    optimizedImage.bytes().length,
                    originalWidth,
                    originalHeight,
                    optimizedImage.width(),
                    optimizedImage.height(),
                    sha256Hex(optimizedImage.bytes()));
        } catch (IOException exception) {
            LOGGER.error("No fue posible procesar la imagen de venta {}", originalFileName, exception);
            throw new ApiRequestException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR_ALMACENAMIENTO",
                    "No fue posible guardar las imagenes de la venta.");
        }
    }

    public String guardarImagen(Long ventaId, String codigoVendido, MultipartFile file) {
        return guardarArchivo(ventaId, file).storedImagePath();
    }

    public StoredVentaDesignFile cargarImagen(String storedPath) {
        return cargarArchivo(storedPath);
    }

    public StoredVentaDesignFile cargarMiniatura(String thumbnailPath, String fallbackImagePath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return cargarArchivo(fallbackImagePath);
        }

        return cargarArchivo(thumbnailPath);
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
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "RUTA_INVALIDA",
                        "No fue posible eliminar la imagen del diseno vendido.");
            }

            if (Files.deleteIfExists(absoluteFilePath)) {
                LOGGER.info("Archivo de venta eliminado: {}", absoluteFilePath);
            }
        } catch (InvalidPathException | IOException exception) {
            LOGGER.error("No fue posible eliminar el archivo almacenado '{}'", storedPath, exception);
            throw new ApiRequestException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR_ALMACENAMIENTO",
                    "No fue posible eliminar las imagenes de la venta.");
        }
    }

    private StoredVentaDesignFile cargarArchivo(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new ResourceNotFoundException("La venta no tiene archivos del diseno adjuntos.");
        }

        try {
            Path normalizedStoredPath = Path.of(storedPath.replace('\\', '/')).normalize();
            Path absoluteFilePath = normalizedStoredPath.isAbsolute()
                    ? normalizedStoredPath
                    : resolverRutaAbsoluta(normalizedStoredPath);

            if (!absoluteFilePath.startsWith(uploadsRoot)) {
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "RUTA_INVALIDA",
                        "No fue posible acceder a la imagen del diseno vendido.");
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
            throw new ApiRequestException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR_ALMACENAMIENTO",
                    "No fue posible acceder a la imagen del diseno vendido.");
        }
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    "ARCHIVO_VACIO",
                    "El archivo seleccionado esta vacio.");
        }
    }

    private void validateOriginalSize(long sizeBytes) {
        if (sizeBytes > maxOriginalFileSizeBytes) {
            throw new ApiRequestException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "ARCHIVO_DEMASIADO_GRANDE",
                    "Una de las imagenes supera el tamano maximo permitido.",
                    Map.of("maximoMb", maxOriginalFileSizeBytes / (1024L * 1024L)));
        }
    }

    private void validatePixelCount(BufferedImage image) {
        long totalPixels = (long) image.getWidth() * image.getHeight();
        if (totalPixels > maxPixelCount) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    "IMAGEN_DIMENSIONES_EXCESIVAS",
                    "La imagen excede el limite de pixeles permitido.");
        }
    }

    private void validateWritableDirectory(Path directory) throws IOException {
        if (!Files.isWritable(directory)) {
            throw new ApiRequestException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ERROR_ALMACENAMIENTO",
                    "No fue posible escribir en el directorio de imagenes de ventas.");
        }
    }

    private void validateTargetPath(Path targetFile, Path expectedParent) {
        if (!targetFile.startsWith(expectedParent)) {
            throw new ApiRequestException(
                    HttpStatus.BAD_REQUEST,
                    "RUTA_INVALIDA",
                    "No fue posible guardar la imagen del diseno vendido.");
        }
    }

    private void writeBytesAtomically(Path targetFile, byte[] bytes) throws IOException {
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        try {
            Files.write(tempFile, bytes);
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(targetFile);
            throw exception;
        }
    }

    private EncodedImage encodeOptimizedImage(BufferedImage baseImage, OutputFormat outputFormat) throws IOException {
        if (outputFormat == OutputFormat.PNG) {
            return encodePngWithinLimit(baseImage);
        }

        return encodeJpegWithinLimit(baseImage);
    }

    private EncodedImage encodeThumbnailImage(BufferedImage thumbnailBaseImage, OutputFormat outputFormat) throws IOException {
        return outputFormat == OutputFormat.PNG
                ? encodePng(thumbnailBaseImage)
                : encodeJpeg(thumbnailBaseImage, Math.min(quality, 0.78f));
    }

    private EncodedImage encodePngWithinLimit(BufferedImage image) throws IOException {
        BufferedImage currentImage = image;

        while (true) {
            EncodedImage encodedImage = encodePng(currentImage);
            if (encodedImage.bytes().length <= maxOptimizedFileSizeBytes) {
                return encodedImage;
            }

            if (Math.max(currentImage.getWidth(), currentImage.getHeight()) <= 600) {
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "IMAGEN_DEMASIADO_GRANDE",
                        "No fue posible optimizar la imagen dentro del limite permitido.");
            }

            currentImage = scaleByFactor(currentImage, 0.88d);
        }
    }

    private EncodedImage encodeJpegWithinLimit(BufferedImage image) throws IOException {
        BufferedImage currentImage = image;
        float currentQuality = quality;

        while (true) {
            EncodedImage encodedImage = encodeJpeg(currentImage, currentQuality);
            if (encodedImage.bytes().length <= maxOptimizedFileSizeBytes) {
                return encodedImage;
            }

            if (currentQuality > 0.58f) {
                currentQuality = Math.max(0.58f, currentQuality - 0.08f);
                continue;
            }

            if (Math.max(currentImage.getWidth(), currentImage.getHeight()) <= 600) {
                throw new ApiRequestException(
                        HttpStatus.BAD_REQUEST,
                        "IMAGEN_DEMASIADO_GRANDE",
                        "No fue posible optimizar la imagen dentro del limite permitido.");
            }

            currentImage = scaleByFactor(currentImage, 0.88d);
        }
    }

    private EncodedImage encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!ImageIO.write(toTargetType(image, true), "png", outputStream)) {
            throw new IOException("No existe un writer PNG disponible.");
        }

        return new EncodedImage(outputStream.toByteArray(), image.getWidth(), image.getHeight());
    }

    private EncodedImage encodeJpeg(BufferedImage image, float compressionQuality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No existe un writer JPEG disponible.");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(clampQuality(compressionQuality));
            }
            writer.write(null, new IIOImage(toTargetType(image, false), null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return new EncodedImage(outputStream.toByteArray(), image.getWidth(), image.getHeight());
    }

    private BufferedImage resizeProportionally(BufferedImage sourceImage, int maxSide) {
        int originalWidth = sourceImage.getWidth();
        int originalHeight = sourceImage.getHeight();
        int longestSide = Math.max(originalWidth, originalHeight);
        if (longestSide <= maxSide) {
            return sourceImage;
        }

        double scale = (double) maxSide / longestSide;
        int targetWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(originalHeight * scale));
        return resizeToDimensions(sourceImage, targetWidth, targetHeight);
    }

    private BufferedImage scaleByFactor(BufferedImage sourceImage, double factor) {
        int targetWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * factor));
        int targetHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * factor));
        return resizeToDimensions(sourceImage, targetWidth, targetHeight);
    }

    private BufferedImage resizeToDimensions(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        boolean preserveAlpha = hasTransparency(sourceImage);
        BufferedImage targetImage = new BufferedImage(
                targetWidth,
                targetHeight,
                preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return targetImage;
    }

    private BufferedImage applyOrientation(BufferedImage sourceImage, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return sourceImage;
        }

        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int targetWidth = width;
        int targetHeight = height;
        AffineTransform transform = new AffineTransform();

        switch (orientation) {
            case 2 -> {
                transform.scale(-1.0d, 1.0d);
                transform.translate(-width, 0.0d);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.scale(1.0d, -1.0d);
                transform.translate(0.0d, -height);
            }
            case 5 -> {
                targetWidth = height;
                targetHeight = width;
                transform.rotate(Math.PI / 2.0d);
                transform.scale(1.0d, -1.0d);
            }
            case 6 -> {
                targetWidth = height;
                targetHeight = width;
                transform.translate(height, 0.0d);
                transform.rotate(Math.PI / 2.0d);
            }
            case 7 -> {
                targetWidth = height;
                targetHeight = width;
                transform.rotate(-Math.PI / 2.0d);
                transform.scale(1.0d, -1.0d);
                transform.translate(-width, 0.0d);
            }
            case 8 -> {
                targetWidth = height;
                targetHeight = width;
                transform.translate(0.0d, width);
                transform.rotate(-Math.PI / 2.0d);
            }
            default -> {
                return sourceImage;
            }
        }

        boolean preserveAlpha = hasTransparency(sourceImage);
        BufferedImage targetImage = new BufferedImage(
                targetWidth,
                targetHeight,
                preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(sourceImage, transform, null);
        } finally {
            graphics.dispose();
        }

        return targetImage;
    }

    private boolean hasTransparency(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }

        int stepX = Math.max(1, image.getWidth() / 32);
        int stepY = Math.max(1, image.getHeight() / 32);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int alpha = (image.getRGB(x, y) >> 24) & 0xFF;
                if (alpha < 255) {
                    return true;
                }
            }
        }

        return false;
    }

    private BufferedImage toTargetType(BufferedImage image, boolean preserveAlpha) {
        int targetType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (image.getType() == targetType) {
            return image;
        }

        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), targetType);
        Graphics2D graphics = convertedImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return convertedImage;
    }

    private int resolveExifOrientation(byte[] originalBytes) {
        if (originalBytes.length < 4
                || (originalBytes[0] & 0xFF) != 0xFF
                || (originalBytes[1] & 0xFF) != 0xD8) {
            return 1;
        }

        int offset = 2;
        while (offset + 4 < originalBytes.length) {
            if ((originalBytes[offset] & 0xFF) != 0xFF) {
                break;
            }

            int marker = originalBytes[offset + 1] & 0xFF;
            offset += 2;
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }

            int length = readUnsignedShort(originalBytes, offset, false);
            if (length < 2 || offset + length > originalBytes.length) {
                break;
            }

            if (marker == 0xE1
                    && length >= 10
                    && "Exif".equals(new String(originalBytes, offset + 2, 4, StandardCharsets.US_ASCII))) {
                int tiffStart = offset + 8;
                return readOrientationFromTiff(originalBytes, tiffStart);
            }

            offset += length;
        }

        return 1;
    }

    private int readOrientationFromTiff(byte[] data, int tiffStart) {
        if (tiffStart + 8 >= data.length) {
            return 1;
        }

        boolean littleEndian;
        if (data[tiffStart] == 'I' && data[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (data[tiffStart] == 'M' && data[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }

        int ifdOffset = readInt(data, tiffStart + 4, littleEndian);
        int ifdStart = tiffStart + ifdOffset;
        if (ifdStart < 0 || ifdStart + 2 > data.length) {
            return 1;
        }

        int entryCount = readUnsignedShort(data, ifdStart, littleEndian);
        int entryOffset = ifdStart + 2;
        for (int index = 0; index < entryCount; index++) {
            int currentEntryOffset = entryOffset + (index * 12);
            if (currentEntryOffset + 12 > data.length) {
                return 1;
            }

            int tag = readUnsignedShort(data, currentEntryOffset, littleEndian);
            if (tag != 0x0112) {
                continue;
            }

            return readUnsignedShort(data, currentEntryOffset + 8, littleEndian);
        }

        return 1;
    }

    private int readUnsignedShort(byte[] data, int offset, boolean littleEndian) {
        int first = data[offset] & 0xFF;
        int second = data[offset + 1] & 0xFF;
        return littleEndian ? (second << 8) | first : (first << 8) | second;
    }

    private int readInt(byte[] data, int offset, boolean littleEndian) {
        int first = data[offset] & 0xFF;
        int second = data[offset + 1] & 0xFF;
        int third = data[offset + 2] & 0xFF;
        int fourth = data[offset + 3] & 0xFF;
        return littleEndian
                ? (fourth << 24) | (third << 16) | (second << 8) | first
                : (first << 24) | (second << 16) | (third << 8) | fourth;
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte currentByte : hash) {
                builder.append(String.format("%02x", currentByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No fue posible calcular el hash de la imagen.", exception);
        }
    }

    private float clampQuality(float rawQuality) {
        return Math.max(0.4f, Math.min(0.95f, rawQuality));
    }

    private String normalizarContentType(String contentType) {
        if (contentType == null) {
            return "";
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(0, separatorIndex).trim();
        }

        return normalized;
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
            String thumbnailFileName,
            String storedImagePath,
            String storedThumbnailPath,
            String mediaType,
            String finalFormat,
            long originalSizeBytes,
            long optimizedSizeBytes,
            int originalWidth,
            int originalHeight,
            int optimizedWidth,
            int optimizedHeight,
            String sha256Hash) {}

    private record EncodedImage(byte[] bytes, int width, int height) {}

    private enum OutputFormat {
        JPEG(".jpg", "image/jpeg", "JPEG"),
        PNG(".png", "image/png", "PNG");

        private final String extension;
        private final String mediaType;
        private final String label;

        OutputFormat(String extension, String mediaType, String label) {
            this.extension = extension;
            this.mediaType = mediaType;
            this.label = label;
        }

        public String extension() {
            return extension;
        }

        public String mediaType() {
            return mediaType;
        }

        public String label() {
            return label;
        }
    }
}
