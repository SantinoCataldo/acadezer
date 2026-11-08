package com.acadezer.api.securityJwt;

import com.acadezer.api.model.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTests {
    private static final String SECRET = "dGVzdC1vbmx5LXNpZ25pbmcta2V5LTMyaXRlbXMtMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private final JwtServiceImpl jwt = new JwtServiceImpl(SECRET, 3600000);
    private UserEntity user(String email) {
        UserEntity user = new UserEntity(); user.setEmail(email); return user;
    }
    @Test void accessTokenHasExpectedSubjectAndRejectsOtherUser() {
        String token = jwt.generateToken(user("ana@example.com"));
        assertThat(jwt.extractUsername(token)).isEqualTo("ana@example.com");
        assertThat(jwt.isTokenValid(token, user("ana@example.com"))).isTrue();
        assertThat(jwt.isTokenValid(token, user("bob@example.com"))).isFalse();
    }
    @Test void rejectsExpiredOrForgedTokens() {
        String expired = new JwtServiceImpl(SECRET, -1000).generateToken(user("ana@example.com"));
        assertThatThrownBy(() -> jwt.extractUsername(expired)).isInstanceOf(ExpiredJwtException.class);
        String forged = Jwts.builder().subject("ana@example.com").signWith(Jwts.SIG.HS256.key().build()).compact();
        assertThatThrownBy(() -> jwt.extractUsername(forged)).isInstanceOf(JwtException.class);
    }
    @Test void refreshOrOtherTokensCannotBeUsedAsAccessTokens() {
        for (String type : new String[]{"refresh", "other"}) {
            String token = Jwts.builder().subject("ana@example.com").claim("type", type)
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
            assertThat(jwt.isTokenValid(token, user("ana@example.com"))).isFalse();
        }
    }
}
