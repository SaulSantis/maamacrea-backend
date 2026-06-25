package com.maamacrea.backend.equipamiento;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipamientos")
@CrossOrigin(origins = "http://localhost:4200")
public class EquipamientoController {

    private final EquipamientoService equipamientoService;

    public EquipamientoController(EquipamientoService equipamientoService) {
        this.equipamientoService = equipamientoService;
    }

    @GetMapping
    public List<EquipamientoResponse> listarTodos() {
        return equipamientoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipamientoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipamientoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EquipamientoResponse> crear(@Valid @RequestBody EquipamientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipamientoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipamientoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipamientoRequest request) {
        return ResponseEntity.ok(equipamientoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        equipamientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
