package com.example.story.repository;

import com.example.story.entity.ContestEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends JpaRepository<ContestEntity, Long> {

    @Query(value = "SELECT * FROM contests WHERE name ILIKE CONCAT('%', :name, '%')", nativeQuery = true)
    List<ContestEntity> findAlikeByName(String name);

    @Query(value = "SELECT * FROM contests WHERE name = :name", nativeQuery = true)
    Optional<ContestEntity> findByName(String name);
}
