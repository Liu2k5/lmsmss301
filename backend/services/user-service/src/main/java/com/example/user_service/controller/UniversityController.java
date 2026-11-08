package com.example.user_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.user_service.dto.UniversityRequest;
import com.example.user_service.dto.UniversityResponse;
import com.example.user_service.entity.University;
import com.example.user_service.repository.UniversityRepository;
import com.example.user_service.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {
    private final UniversityRepository universities;
    private final UserRepository users;

    @GetMapping
    public List<UniversityResponse> list() {
        return universities.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UniversityResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UniversityResponse create(@Valid @RequestBody UniversityRequest r) {
        University u = new University();
        u.setName(r.name());
        u.setAddress(r.address());
        return toResponse(universities.save(u));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public UniversityResponse update(@PathVariable Long id, @Valid @RequestBody UniversityRequest r) {
        University u = find(id);
        u.setName(r.name());
        u.setAddress(r.address());
        return toResponse(u);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void delete(@PathVariable Long id) {
        if (users.existsByUniversity_Id(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "University still has users");
        universities.delete(find(id));
    }

    private University find(Long id) {
        return universities.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found"));
    }

    private UniversityResponse toResponse(University u) {
        return new UniversityResponse(u.getId(), u.getName(), u.getAddress());
    }
}