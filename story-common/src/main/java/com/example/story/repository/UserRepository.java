package com.example.story.repository;

import com.example.story.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query(value = "SELECT COUNT(*) FROM users WHERE email ILIKE CONCAT('%', :email, '%')", nativeQuery = true)
    Long countAlikeByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE email ILIKE CONCAT('%', :email, '%')", nativeQuery = true)
    List<UserEntity> findAlikeByEmail(String email);

    @Query(value = "SELECT * FROM users LIMIT :_limit OFFSET :_offset", nativeQuery = true)
    List<UserEntity> findAllLimited(Long _limit, Long _offset);

    @Query(value = "SELECT * FROM users WHERE email ILIKE CONCAT('%', :email, '%') LIMIT :_limit OFFSET :_offset", nativeQuery = true)
    List<UserEntity> findAlikeByEmailLimited(String email, Long _limit, Long _offset);

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<UserEntity> findByEmail(String email);

    @Query(value = """
            SELECT DISTINCT ur.role_name
            FROM user_roles ur
            JOIN users u ON u.id = ur.user_id
            WHERE u.email = :email
            ORDER BY ur.role_name
            """, nativeQuery = true)
    List<String> findRoleNamesByEmail(String email);

    @Query(value = """
            SELECT DISTINCT rp.permission_name
            FROM user_roles ur
            JOIN role_permissions rp ON rp.role_name = ur.role_name
            JOIN users u ON u.id = ur.user_id
            WHERE u.email = :email
            ORDER BY rp.permission_name
            """, nativeQuery = true)
    List<String> findPermissionNamesByEmail(String email);

    @Query(value = "SELECT role_name FROM user_roles WHERE user_id = :userId ORDER BY role_name", nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT name FROM roles ORDER BY name", nativeQuery = true)
    List<String> findAllRoleNames();

    @Query(value = "SELECT COUNT(*) FROM user_roles WHERE role_name = :roleName", nativeQuery = true)
    long countUsersWithRole(@Param("roleName") String roleName);

    @Modifying
    @Query(value = """
            INSERT INTO user_roles (user_id, role_name)
            VALUES (:userId, :roleName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void assignRole(@Param("userId") Long userId, @Param("roleName") String roleName);

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId", nativeQuery = true)
    void removeAllRoles(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO role_change_audit
                (actor_user_id, target_user_id, previous_roles, new_roles)
            VALUES (:actorId, :targetId, :previousRoles, :newRoles)
            """, nativeQuery = true)
    void auditRoleChange(@Param("actorId") Long actorId,
                         @Param("targetId") Long targetId,
                         @Param("previousRoles") String previousRoles,
                         @Param("newRoles") String newRoles);
}
