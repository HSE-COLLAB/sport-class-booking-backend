package ru.hse.sportclassbookingbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupPatchRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupRequest;
import ru.hse.sportclassbookingbackend.dto.healthgroup.HealthGroupResponse;
import ru.hse.sportclassbookingbackend.service.HealthGroupService;

import java.util.List;

@RestController
@RequestMapping("/api/health-groups")
@RequiredArgsConstructor
public class HealthGroupController {
    private final HealthGroupService healthGroupService;

    @GetMapping
    public ResponseEntity<List<HealthGroupResponse>> getAll(){
        return ResponseEntity.ok(healthGroupService.getAll());
    }

    @PostMapping
    public ResponseEntity<HealthGroupResponse> create(@RequestBody @Valid HealthGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(healthGroupService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HealthGroupResponse> update(
            @PathVariable int id,
            @RequestBody HealthGroupPatchRequest request
    ) {
        return ResponseEntity.ok(healthGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        healthGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
