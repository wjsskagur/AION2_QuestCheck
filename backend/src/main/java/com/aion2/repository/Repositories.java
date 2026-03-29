package com.aion2.repository;

import com.aion2.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<Account> findByAuthProviderAndProviderId(Account.AuthProvider authProvider, String providerId);
}

public interface CharacterRepository extends JpaRepository<Character, Long> {
    List<Character> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    Optional<Character> findByIdAndAccountId(Long id, Long accountId);
    long countByAccountId(Long accountId);
    List<Character> findTop50ByServerOrderByCombatPowerDesc(String server);
    @Query("SELECT COUNT(DISTINCT c.accountId) FROM Character c")
    long countDistinctAccountId();
}

public interface QuestTemplateRepository extends JpaRepository<QuestTemplate, Long> {
    @Query("SELECT q FROM QuestTemplate q WHERE q.isActive = true AND (q.minLevel IS NULL OR q.minLevel <= :lv) AND (q.maxLevel IS NULL OR q.maxLevel >= :lv) ORDER BY q.type, q.name")
    List<QuestTemplate> findApplicable(@Param("lv") int level);
    List<QuestTemplate> findByIsActiveTrueOrderByTypeAscNameAsc();
}

public interface QuestCheckRepository extends JpaRepository<QuestCheck, Long> {
    boolean existsByCharacterIdAndQuestIdAndDateKey(Long characterId, Long questId, LocalDateTime dateKey);
    Optional<QuestCheck> findByCharacterIdAndQuestIdAndDateKey(Long characterId, Long questId, LocalDateTime dateKey);
}

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByCharacterIdAndStatusOrderByCreatedAtDesc(Long characterId, VerificationCode.Status status);
    @Modifying @Transactional
    @Query("UPDATE VerificationCode v SET v.status = 'EXPIRED' WHERE v.characterId = :id AND v.status = 'PENDING'")
    void expirePendingByCharacterId(@Param("id") Long characterId);
}

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategory(Post.Category category, Pageable pageable);
    boolean existsBySourceUrl(String sourceUrl);
}

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    default String getValue(String key, String def) {
        return findById(key).map(SystemConfig::getConfigValue).orElse(def);
    }
}

// ── 파티 모집 게시판 ───────────────────────────────────────────────────────────
public interface PartyCategoryRepository extends JpaRepository<PartyCategory, Long> {
    List<PartyCategory> findByIsActiveTrueOrderBySortOrderAscNameAsc();
    boolean existsByName(String name);
}

public interface PartySubcategoryRepository extends JpaRepository<PartySubcategory, Long> {
    List<PartySubcategory> findByCategoryIdAndIsActiveTrueOrderBySortOrderAscNameAsc(Long categoryId);
    List<PartySubcategory> findByCategoryId(Long categoryId);
    boolean existsByCategoryIdAndName(Long categoryId, String name);
}

public interface PartyPostRepository extends JpaRepository<PartyPost, Long> {

    @Query("""
        SELECT p FROM PartyPost p
        WHERE p.status != 'DELETED'
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategoryId = :subcategoryId)
          AND (:server IS NULL OR p.server = :server)
        ORDER BY p.createdAt DESC
    """)
    Page<PartyPost> search(
        @Param("categoryId") Long categoryId,
        @Param("subcategoryId") Long subcategoryId,
        @Param("server") String server,
        Pageable pageable);

    @Modifying @Transactional
    @Query("UPDATE PartyPost p SET p.views = p.views + 1 WHERE p.id = :id")
    void incrementViews(@Param("id") Long id);
}

public interface PartyCommentRepository extends JpaRepository<PartyComment, Long> {
    List<PartyComment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
    long countByPostIdAndIsDeletedFalse(Long postId);
}
