package ru.netology.storagecloud.repositories.tokens;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.storagecloud.repositories.database.entities.TokenEntity;

public interface TokenJpaRepository extends JpaRepository<TokenEntity, String> {
}
