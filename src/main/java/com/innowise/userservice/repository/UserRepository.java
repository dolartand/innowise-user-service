package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Named methods

    Optional<User> findByEmail(String email);

    List<User> findByActive(Boolean active);

    List<User> findByNameAndSurname(String name, String surname);

    boolean existsByEmail(String email);

    // JPQL

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchParam, '%')) OR " +
            "LOWER(u.surname) LIKE LOWER(CONCAT('%', :searchParam, '%'))")
    Page<User> searchByNameOrSurname(@Param("searchParam") String searchParam, Pageable pageable);

    @Query("SELECT COUNT(c) FROM User u JOIN u.cards c WHERE u.id = :userId")
    long countCardsByUserId(@Param("userId") Long userId);

    // Native sql

    @Modifying
    @Query(value = "UPDATE users SET active = true WHERE id = :userId", nativeQuery = true)
    int activateUser(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE users SET active = false WHERE id = :userId", nativeQuery = true)
    int deactivateUser(@Param("userId") Long userId);
}
