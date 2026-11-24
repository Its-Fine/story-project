package com.example.story.auth.repository;

import com.example.story.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO user_roles (user_id, role_name)
            VALUES (:userId, :roleName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void assignRole(@Param("userId") Long userId, @Param("roleName") String roleName);

    @Query(value = "SELECT role_name FROM user_roles WHERE user_id = :userId ORDER BY role_name", nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT DISTINCT rp.permission_name
            FROM user_roles ur
            JOIN role_permissions rp ON rp.role_name = ur.role_name
            WHERE ur.user_id = :userId
            ORDER BY rp.permission_name
            """, nativeQuery = true)
    List<String> findPermissionNamesByUserId(@Param("userId") Long userId);
}
