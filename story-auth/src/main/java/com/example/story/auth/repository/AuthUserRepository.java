package com.example.story.auth.repository;

import com.example.story.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByOidcIssuerAndOidcSubject(String oidcIssuer, String oidcSubject);

    @Modifying
    @Query(value = """
            INSERT INTO user_roles (user_id, role_name)
            VALUES (:userId, :roleName)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void assignRole(@Param("userId") Long userId, @Param("roleName") String roleName);
}
