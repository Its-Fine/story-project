package com.example.story.repository;

import com.example.story.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    @Query(value = "SELECT permission_name FROM role_permissions WHERE role_name = :roleName ORDER BY permission_name", nativeQuery = true)
    List<String> findPermissionNames(@Param("roleName") String roleName);

    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE role_name = :roleName", nativeQuery = true)
    long countAssignedUsers(@Param("roleName") String roleName);

    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_name = :roleName", nativeQuery = true)
    void removePermissions(@Param("roleName") String roleName);

    @Modifying
    @Query(value = """
            INSERT INTO role_permissions (role_name, permission_name)
            VALUES (:roleName, :permissionName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void assignPermission(@Param("roleName") String roleName,
                          @Param("permissionName") String permissionName);

    @Modifying
    @Query(value = """
            INSERT INTO role_definition_audit
                (actor_user_id, role_name, action, previous_permissions, new_permissions)
            VALUES (:actorId, :roleName, :action, :previousPermissions, :newPermissions)
            """, nativeQuery = true)
    void auditDefinitionChange(@Param("actorId") Long actorId,
                               @Param("roleName") String roleName,
                               @Param("action") String action,
                               @Param("previousPermissions") String previousPermissions,
                               @Param("newPermissions") String newPermissions);
}
