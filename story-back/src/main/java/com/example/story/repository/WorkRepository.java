package com.example.story.repository;

import com.example.story.entity.WorkEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkRepository extends JpaRepository<WorkEntity, Long> {

    @Query(value = "SELECT COALESCE(SUM(file_size), 0) FROM works WHERE application_id = :applicationId", nativeQuery = true)
    Integer sumSizeByApplicationId(Long applicationId);

    @Query(value = "SELECT * FROM works WHERE application_id = :applicationId", nativeQuery = true)
    List<WorkEntity> findByApplicationId(Long applicationId);

    @Query(value = "SELECT COUNT(*) FROM works WHERE application_id = :applicationId", nativeQuery = true)
    Integer countByApplicationId(Long applicationId);

    @Query(value = "SELECT * FROM works WHERE name = :name AND application_id = :applicationId", nativeQuery = true)
    Optional<WorkEntity> findByNameAndApplicationId(String name, Long applicationId);
}
