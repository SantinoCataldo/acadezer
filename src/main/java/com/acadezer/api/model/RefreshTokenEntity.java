package com.acadezer.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
public class RefreshTokenEntity {
    @Id @Column(length = 64) private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false) private UserEntity user;
    @Column(nullable = false) private Instant expiresAt;
}
