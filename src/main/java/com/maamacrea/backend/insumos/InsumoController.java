package com.maamacrea.backend.insumos;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/insumos")
@CrossOrigin(origins = "http://localhost:4200")
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @GetMapping
    public List<InsumoResponse> listarTodos() {
        return insumoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsumoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<InsumoResponse> crear(@Valid @RequestBody InsumoRequest insumoRequest) {
        InsumoResponse creado = insumoService.crear(insumoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping(path = "/{id}/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InsumoResponse> subirDocumento(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(insumoService.actualizarDocumento(id, file));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsumoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody InsumoRequest insumoRequest) {
        return ResponseEntity.ok(insumoService.actualizar(id, insumoRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        insumoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
