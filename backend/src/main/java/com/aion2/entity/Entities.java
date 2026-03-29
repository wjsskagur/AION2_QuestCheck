package com.aion2.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ── Account ──────────────────────────────────────────────────────────────────
@Entity @Table(name = "account")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { ADMIN, USER }
}

// ── Character ─────────────────────────────────────────────────────────────────
@Entity @Table(name = "`character`")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class Character {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 50)
    private String server;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "class_name", length = 30)
    private String className;

    @Column(nullable = false)
    @Builder.Default
    private int level = 1;

    @Column(name = "combat_power", nullable = false)
    @Builder.Default
    private long combatPower = 0;

    private String grade;

    @Column(name = "grade_color", length = 10)
    private String gradeColor;

    @Column(length = 30)
    private String wings;

    @Builder.Default
    private long honor = 0;

    @Column(columnDefinition = "JSON")
    private String rawData;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    private LocalDateTime verifiedAt;
    private LocalDateTime fetchedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

// ── QuestTemplate ─────────────────────────────────────────────────────────────
@Entity @Table(name = "quest_template")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class QuestTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestType type;

    private Integer minLevel;
    private Integer maxLevel;

    @Column(length = 20)
    private String days;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum QuestType { DAILY, WEEKLY, SPECIFIC }
}

// ── QuestCheck ────────────────────────────────────────────────────────────────
@Entity @Table(name = "quest_check")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class QuestCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @Column(name = "date_key", nullable = false)
    private LocalDate dateKey;

    @Column(name = "is_done", nullable = false)
    @Builder.Default
    private boolean isDone = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime checkedAt = LocalDateTime.now();
}

// ── VerificationCode ──────────────────────────────────────────────────────────
@Entity @Table(name = "verification_code")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class VerificationCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public enum Status { PENDING, VERIFIED, EXPIRED }
}

// ── Post ──────────────────────────────────────────────────────────────────────
@Entity @Table(name = "post")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Category { NOTICE, UPDATE, EVENT }
}

// ── SystemConfig ──────────────────────────────────────────────────────────────
@Entity @Table(name = "system_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class SystemConfig {
    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    private LocalDateTime updatedAt;

    @PreUpdate @PrePersist
    void touch() { this.updatedAt = LocalDateTime.now(); }
}
