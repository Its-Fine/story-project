package com.example.story.repository;

import com.example.story.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
