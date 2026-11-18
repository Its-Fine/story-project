package com.example.story.repository;

import com.example.story.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    @Query(value = "SELECT * FROM members WHERE application_id = :applicationId", nativeQuery = true)
    List<MemberEntity> findByApplicationId(Long applicationId);

    @Query(value = "SELECT COUNT(*) FROM members WHERE application_id = :applicationId", nativeQuery = true)
    Integer countByApplicationId(Long applicationId);

    @Modifying
    @Query(value = "DELETE FROM members WHERE application_id = :applicationId", nativeQuery = true)
    void deleteByApplicationId(Long applicationId);
}
