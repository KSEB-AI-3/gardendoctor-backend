package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.UserPlantRequestDto;
import com.project.farming.domain.plant.dto.UserPlantResponseDto;
import com.project.farming.domain.plant.service.UserPlantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/users/plants")
@RequiredArgsConstructor
@RestController
public class UserPlantController {

    private final UserPlantService userPlantService;

    @PostMapping
    public ResponseEntity<UserPlantResponseDto> createUserPlant(@Valid @RequestBody UserPlantRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPlantService.saveUserPlant(request));
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<UserPlantResponseDto>> getAllUserPlants(@PathVariable String email) {
        return ResponseEntity.ok(userPlantService.findAllUserPlants(email));
    }

    @GetMapping("/{email}/{nickname}")
    public ResponseEntity<UserPlantResponseDto> getUserPlant(
            @PathVariable String email, @PathVariable String nickname) {
        return ResponseEntity.ok(userPlantService.findUserPlantByName(email, nickname));
    }

    @PutMapping("/{email}/{nickname}")
    public ResponseEntity<UserPlantResponseDto> updateUserPlant(
            @PathVariable String email, @PathVariable String nickname, @Valid @RequestBody UserPlantRequestDto request) {
        return ResponseEntity.ok(userPlantService.updateUserPlant(email, nickname, request));
    }

    @DeleteMapping("/{email}/{nickname}")
    public ResponseEntity<UserPlantResponseDto> deleteUserPlant(
            @PathVariable String email, @PathVariable String nickname) {
        userPlantService.deleteUserPlant(email, nickname);
        return ResponseEntity.noContent().build();
    }
}
