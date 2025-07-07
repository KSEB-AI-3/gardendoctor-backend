package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.PlantRequestDto;
import com.project.farming.domain.plant.dto.PlantResponseDto;
import com.project.farming.domain.plant.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/plants")
@RequiredArgsConstructor
@RestController
public class PlantController {

    private final PlantService plantService;

    @PostMapping
    public ResponseEntity<PlantResponseDto> createPlant(@RequestBody PlantRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantService.savePlant(request));
    }

    @GetMapping
    public ResponseEntity<List<PlantResponseDto>> getAllPlants() {
        return ResponseEntity.ok(plantService.findAllPlants());
    }

    @GetMapping("/{name}")
    public ResponseEntity<PlantResponseDto> getPlant(@PathVariable String name) {
        return ResponseEntity.ok(plantService.findPlantByName(name));
    }

    @PutMapping("/{name}")
    public ResponseEntity<PlantResponseDto> updatePlant(@PathVariable String name, @RequestBody PlantRequestDto request) {
        return ResponseEntity.ok(plantService.updatePlant(name, request));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<PlantResponseDto> deletePlant(@PathVariable String name) {
        plantService.deletePlant(name);
        return ResponseEntity.noContent().build();
    }
}
