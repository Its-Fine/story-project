package com.example.story.controller.admin;

import com.example.story.dto.access.RequestRoleDto;
import com.example.story.dto.errors.ResponseErrorDto;
import com.example.story.exception.ResourceNotFoundException;
import com.example.story.service.admin.AccessManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/access")
@PreAuthorize("hasAuthority(T(com.example.story.security.Permissions).ROLE_MANAGE)")
@RequiredArgsConstructor
public class AccessManagementController {

    private final AccessManagementService accessManagementService;

    @GetMapping("/permissions")
    public ResponseEntity<?> permissions() {
        return ResponseEntity.ok(accessManagementService.obtainPermissions());
    }

    @GetMapping("/roles")
    public ResponseEntity<?> roles() {
        return ResponseEntity.ok(accessManagementService.obtainRoles());
    }

    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody @Validated RequestRoleDto dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(accessManagementService.createRole(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @PutMapping("/roles/{roleName}")
    public ResponseEntity<?> updateRole(@PathVariable String roleName,
                                        @RequestBody @Validated RequestRoleDto dto) {
        try {
            return ResponseEntity.ok(accessManagementService.updateRole(roleName, dto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseErrorDto(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResponseErrorDto(e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<?> deleteRole(@PathVariable String roleName) {
        try {
            accessManagementService.deleteRole(roleName);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseErrorDto(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ResponseErrorDto(e.getMessage()));
        }
    }
}
