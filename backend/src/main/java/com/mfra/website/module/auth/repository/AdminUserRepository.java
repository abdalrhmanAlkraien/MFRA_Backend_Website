package com.mfra.website.module.auth.repository;

import com.mfra.website.module.auth.entity.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUserEntity, UUID> {

    @Query("SELECT u FROM AdminUserEntity u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<AdminUserEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(@Param("email") String email);

    Optional<AdminUserEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
