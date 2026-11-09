package com.example.story.repository;

import com.example.story.entity.ApplicationEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query(value = "SELECT * FROM applications LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
    List<ApplicationEntity> findAllLimited(Long _limit, Long _offset);

    @Query(value = "SELECT * FROM applications WHERE user_id = :userId LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
    List<ApplicationEntity> findByUserIdLimited(Long userId, Long _limit, Long _offset);

    @Query(value = "SELECT * FROM applications WHERE contest_id = :conId LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
    List<ApplicationEntity> findByContestIdLimited(Long conId, Long _limit, Long _offset);

    @Query(value = "SELECT * FROM applications WHERE user_id = :userId AND contest_id = :conId LIMIT :uLimit OFFSET :uOffset", nativeQuery = true)
    List<ApplicationEntity> findByUserIdAndContestIdLimited(Long userId, Long conId, Long _limit, Long _offset);

    @Query(value = "SELECT * FROM applications WHERE name = :name AND contest_id = :conId", nativeQuery = true)
    Optional<ApplicationEntity> findByNameAndContestId(String name, Long conId);
}
