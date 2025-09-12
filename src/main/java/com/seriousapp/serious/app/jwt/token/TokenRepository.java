package com.seriousapp.serious.app.jwt.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    List<Token> findAllValidTokensByUserId(Long id);
    Optional<Token> findByToken(String token);
}
