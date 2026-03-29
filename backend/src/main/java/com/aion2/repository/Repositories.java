package com.aion2.repository;

import com.aion2.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ── AccountRepository ─────────────────────────────────────────────────────────
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
}

// ── CharacterRepository ───────────────────────────────────────────────────────
public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    Optional<Character> findByIdAndAccountId(Long id, Long accountId);
    long countByAccountId(Long accountId);
    List<Character> findTop50ByServerOrderByCombatPowerDesc(String server);

    @Query("SELECT COUNT(DISTINCT c.accountId) FROM Character c")
    long countDistinctAccountId();
}

// ── QuestTemplateRepository ───────────────────────────────────────────────────
public interface QuestTemplateRepository extends JpaRepository<QuestTemplate, Long> {
    @Query("""
        SELECT q FROM QuestTemplate q
        WHERE q.isActive = true
          AND (q.minLevel IS NULL OR q.minLevel <= :level)
          AND (q.maxLevel IS NULL OR q.maxLevel >= :level)
        ORDER BY q.type, q.name
    """)
    List<QuestTemplate> findApplicable(@Param("level") int level);

    List<QuestTemplate> findByIsActiveTrueOrderByTypeAscNameAsc();
}

// ── QuestCheckRepository ──────────────────────────────────────────────────────
public interface QuestCheckRepository extends JpaRepository<QuestCheck, Long> {
    boolean existsByCharacterIdAndQuestIdAndDateKey(Long characterId, Long questId, LocalDate dateKey);
    Optional<QuestCheck> findByCharacterIdAndQuestIdAndDateKey(Long characterId, Long questId, LocalDate dateKey);
    List<QuestCheck> findByCharacterIdAndDateKeyIn(Long characterId, List<LocalDate> dateKeys);
}

// ── VerificationCodeRepository ────────────────────────────────────────────────
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByCharacterIdAndStatusOrderByCreatedAtDesc(
        Long characterId, VerificationCode.Status status);

    @Modifying @Transactional
    @Query("UPDATE VerificationCode v SET v.status = 'EXPIRED' WHERE v.characterId = :charId AND v.status = 'PENDING'")
    void expirePendingByCharacterId(@Param("charId") Long characterId);
}

// ── PostRepository ────────────────────────────────────────────────────────────
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategory(Post.Category category, Pageable pageable);
    boolean existsBySourceUrl(String sourceUrl);
}

// ── SystemConfigRepository ────────────────────────────────────────────────────
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    default String getValue(String key, String defaultValue) {
        return findById(key).map(SystemConfig::getConfigValue).orElse(defaultValue);
    }
}
