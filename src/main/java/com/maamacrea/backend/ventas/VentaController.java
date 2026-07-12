package com.maamacrea.backend.ventas;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        VentaImagenStorageService.StoredVentaDesignFile storedFile = ventaService.obtenerImagenDiseno(id);
        String dispositionType = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .contentType(storedFile.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + storedFile.fileName() + "\"")
                .body(storedFile.resource());
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request));
    }

    @PutMapping("/{id}")
    public VentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody VentaRequest request) {
        return ventaService.actualizar(id, request);
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
}
