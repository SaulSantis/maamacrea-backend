package com.maamacrea.backend.ventas;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "http://localhost:4200")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public List<VentaResponse> listarTodas() {
        return ventaService.listarTodas();
    }

    @GetMapping("/ultimas")
    public List<VentaResponse> listarUltimas(@RequestParam(defaultValue = "5") int limit) {
        return ventaService.listarUltimas(limit);
    }

    @GetMapping("/{id}")
    public VentaResponse buscarPorId(@PathVariable Long id) {
        return ventaService.buscarPorId(id);
    }

    @GetMapping({"/{id}/archivo-diseno", "/{id}/imagen-diseno"})
    public ResponseEntity<Resource> descargarArchivoDiseno(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download) {
        return buildFileResponse(ventaService.obtenerImagenDiseno(id), download);
    }

    @GetMapping("/{ventaId}/archivos/{archivoId}")
    public ResponseEntity<Resource> descargarArchivoVenta(
            @PathVariable Long ventaId,
            @PathVariable Long archivoId,
            @RequestParam(defaultValue = "false") boolean download) {
        return buildFileResponse(ventaService.obtenerArchivoDiseno(ventaId, archivoId), download);
    }

    @GetMapping("/{ventaId}/archivos/{archivoId}/miniatura")
    public ResponseEntity<Resource> descargarMiniaturaVenta(
            @PathVariable Long ventaId,
            @PathVariable Long archivoId) {
        return buildFileResponse(ventaService.obtenerMiniaturaDiseno(ventaId, archivoId), false);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VentaResponse> crearConArchivos(
            @Valid @RequestPart("datos") VentaRequest request,
            @RequestPart(value = "archivosNuevos", required = false) List<MultipartFile> archivosNuevos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request, archivosNuevos));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public VentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody VentaRequest request) {
        return ventaService.actualizar(id, request);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VentaResponse actualizarConArchivos(
            @PathVariable Long id,
            @Valid @RequestPart("datos") VentaRequest request,
            @RequestPart(value = "archivosNuevos", required = false) List<MultipartFile> archivosNuevos,
            @RequestPart(value = "archivosEliminar", required = false) List<Long> archivosEliminar) {
        return ventaService.actualizar(id, request, archivosEliminar, archivosNuevos);
    }

    @PostMapping(path = "/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VentaResponse agregarArchivos(
            @PathVariable Long id,
            @RequestPart(value = "archivosNuevos", required = false) List<MultipartFile> archivos) {
        return ventaService.agregarArchivos(id, archivos);
    }

    @PostMapping(path = {"/{id}/archivo-diseno", "/{id}/imagen-diseno"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VentaResponse subirArchivoDiseno(
            @PathVariable Long id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile legacyFile) {
        MultipartFile uploadedFile = archivo != null && !archivo.isEmpty() ? archivo : legacyFile;
        return ventaService.actualizarImagenDiseno(id, uploadedFile);
    }

    @PatchMapping("/{id}/estado")
    public VentaResponse actualizarEstado(@PathVariable Long id, @Valid @RequestBody VentaEstadoUpdateRequest request) {
        return ventaService.actualizarEstado(id, request);
    }

    @DeleteMapping("/{ventaId}/archivos/{archivoId}")
    public VentaResponse eliminarArchivo(@PathVariable Long ventaId, @PathVariable Long archivoId) {
        return ventaService.eliminarArchivo(ventaId, archivoId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> buildFileResponse(
            VentaImagenStorageService.StoredVentaDesignFile storedFile,
            boolean download) {
        String dispositionType = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .contentType(storedFile.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + storedFile.fileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(storedFile.resource());
    }
}
