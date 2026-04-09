package ru.hse.sportclassbookingbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.service.HealthGroupService;

import java.util.List;

@RestController
@RequestMapping("/health-groups")
@RequiredArgsConstructor
public class HealthGroupController {

    private final HealthGroupService healthGroupService;

    @GetMapping
    public ResponseEntity<List<HealthGroupResponse>> getAll() {
        return ResponseEntity.ok(healthGroupService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthGroupResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(healthGroupService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthGroupResponse> update(
            @PathVariable int id,
            @RequestBody @Valid HealthGroupRequest request
    ) {
        return ResponseEntity.ok(healthGroupService.update(id, request));
    }
}
