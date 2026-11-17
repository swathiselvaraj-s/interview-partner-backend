package com.interviewpartner.repository;

import com.interviewpartner.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.lastLoginAt < :date")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date")
    Long countNewUsersSince(@Param("date") LocalDateTime date);

    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    @Query("SELECT u FROM User u WHERE u.preferredRole = :role")
    List<User> findByPreferredRole(@Param("role") String role);

    // Search methods
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:role IS NULL OR u.preferredRole = :role) AND " +
            "(:difficulty IS NULL OR u.preferredDifficulty = :difficulty) AND " +
            "(:minExp IS NULL OR u.experienceYears >= :minExp) AND " +
            "(:maxExp IS NULL OR u.experienceYears <= :maxExp) AND " +
            "(:active IS NULL OR u.active = :active)")
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("role") String role,
                           @Param("difficulty") String difficulty,
                           @Param("minExp") Integer minExp,
                           @Param("maxExp") Integer maxExp,
                           @Param("active") Boolean active,
                           Pageable pageable);
}