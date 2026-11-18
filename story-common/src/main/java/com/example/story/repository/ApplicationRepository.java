package com.example.story.repository;

import com.example.story.entity.ApplicationEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    @Query(value = "SELECT COUNT(*) FROM applications WHERE user_id = :userId", nativeQuery = true)
    Long countByUserId(Long userId);

    @Query(value = "SELECT COUNT(*) FROM applications WHERE contest_id = :conId", nativeQuery = true)
    Long countByContestId(Long conId);

    @Query(value = "SELECT COUNT(*) FROM applications WHERE user_id = :userId AND contest_id = :conId", nativeQuery = true)
    Long countByUserIdAndContestId(Long userId, Long conId);

    @Query(value = "SELECT * FROM applications WHERE user_id = :userId", nativeQuery = true)
    List<ApplicationEntity> findByUserId(Long userId);

    @Query(value = "SELECT * FROM applications WHERE contest_id = :conId", nativeQuery = true)
    List<ApplicationEntity> findByContestId(Long conId);

    @Query(value = "SELECT * FROM applications WHERE user_id = :userId AND contest_id = :conId", nativeQuery = true)
    List<ApplicationEntity> findByUserIdAndContestId(Long userId, Long conId);

        @Query(value = "SELECT * FROM applications ORDER BY created_at DESC LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
        List<ApplicationEntity> findAllLimited(@Param("uLimit") Long limit, @Param("uOffset") Long offset);

        @Query(value = "SELECT * FROM applications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
        List<ApplicationEntity> findByUserIdLimited(@Param("userId") Long userId, @Param("uLimit") Long limit, @Param("uOffset") Long offset);

        @Query(value = "SELECT * FROM applications WHERE contest_id = :conId ORDER BY created_at DESC LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
        List<ApplicationEntity> findByContestIdLimited(@Param("conId") Long contestId, @Param("uLimit") Long limit, @Param("uOffset") Long offset);

        @Query(value = "SELECT * FROM applications WHERE user_id = :userId AND contest_id = :conId ORDER BY created_at DESC LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
        List<ApplicationEntity> findByUserIdAndContestIdLimited(
            @Param("userId") Long userId,
            @Param("conId") Long contestId,
            @Param("uLimit") Long limit,
            @Param("uOffset") Long offset
        );

    @Query(value = "SELECT * FROM applications WHERE name = :name AND contest_id = :conId", nativeQuery = true)
    Optional<ApplicationEntity> findByNameAndContestId(String name, Long conId);
}
