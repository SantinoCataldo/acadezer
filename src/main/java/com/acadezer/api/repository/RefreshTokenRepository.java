package com.acadezer.api.repository;

import com.acadezer.api.model.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t join fetch t.user where t.tokenHash = :hash")
    Optional<RefreshTokenEntity> findForUpdate(String hash);
    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :now")
    void deleteExpired(Instant now);
}
