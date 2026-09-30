package com.hospital.billing.security;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    /**
     * Usernames are unique across ALL hospitals (one login name, one JWT subject).
     * Native SQL on purpose: Hibernate's tenant filter does not apply, so a clash with a
     * user of another hospital is detected too.
     */
    @Query(value = "select count(*) from app_user where lower(username) = lower(:username)", nativeQuery = true)
    long countByUsernameAcrossTenants(@Param("username") String username);

    @Query(value = "select count(*) from app_user where lower(email) = lower(:email)", nativeQuery = true)
    long countByEmailAcrossTenants(@Param("email") String email);
}
