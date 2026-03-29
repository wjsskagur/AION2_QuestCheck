package com.aion2.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

// ── Account ───────────────────────────────────────────────────────────────────
@Entity @Table(name = "account")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, length = 50) private String username;
    @Column(length = 255)              private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private Role role = Role.USER;
    @Enumerated(EnumType.STRING) @Column(name = "auth_provider", nullable = false) @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;
    @Column(name = "provider_id", length = 255) private String providerId;
    @Column(length = 255)              private String email;
    @Column(length = 100)              private String nickname;
    @Column(name = "profile_image", length = 500) private String profileImage;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) return nickname;
        if (username != null && !username.isBlank()) return username;
        return "회원#" + id;
    }
    public enum Role { ADMIN, USER }
    public enum AuthProvider { LOCAL, KAKAO, NAVER, GOOGLE, ADMIN }
}

// ── Character ─────────────────────────────────────────────────────────────────
@Entity @Table(name = "`character`")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Character {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(nullable = false, length = 50) private String server;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "class_name", length = 30) private String className;
    @Column(nullable = false) @Builder.Default private int level = 1;
    @Column(name = "combat_power", nullable = false) @Builder.Default private long combatPower = 0;
    private String grade;
    @Column(name = "grade_color", length = 10) private String gradeColor;
    @Column(length = 30) private String wings;
    @Builder.Default private long honor = 0;
    @Column(columnDefinition = "JSON") private String rawData;
    @Column(nullable = false) @Builder.Default private boolean verified = false;
    private LocalDateTime verifiedAt;
    private LocalDateTime fetchedAt;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

// ── QuestTemplate ─────────────────────────────────────────────────────────────
@Entity @Table(name = "quest_template")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QuestType type;
    private Integer minLevel;
    private Integer maxLevel;
    @Column(length = 20) private String days;
    @Column(nullable = false) @Builder.Default private int resetDay = 3;
    @Column(nullable = false) @Builder.Default private int resetHour = 5;
    @Column(nullable = false) @Builder.Default private boolean isActive = true;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    public enum QuestType { DAILY, WEEKLY, SPECIFIC }
}

// ── QuestCheck ────────────────────────────────────────────────────────────────
@Entity @Table(name = "quest_check")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "character_id", nullable = false) private Long characterId;
    @Column(name = "quest_id", nullable = false) private Long questId;
    @Column(name = "date_key", nullable = false) private LocalDateTime dateKey;
    @Column(name = "is_done", nullable = false) @Builder.Default private boolean isDone = true;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime checkedAt = LocalDateTime.now();
}

// ── VerificationCode ──────────────────────────────────────────────────────────
@Entity @Table(name = "verification_code")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "character_id", nullable = false) private Long characterId;
    @Column(nullable = false, unique = true, length = 12) private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private Status status = Status.PENDING;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public enum Status { PENDING, VERIFIED, EXPIRED }
}

// ── Post (공지 게시판) ─────────────────────────────────────────────────────────
@Entity @Table(name = "post")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "source_url", length = 500) private String sourceUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Category category;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    public enum Category { NOTICE, UPDATE, EVENT }
}

// ── SystemConfig ──────────────────────────────────────────────────────────────
@Entity @Table(name = "system_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemConfig {
    @Id @Column(name = "config_key", length = 100) private String configKey;
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT") private String configValue;
    private LocalDateTime updatedAt;
    @PreUpdate @PrePersist void touch() { this.updatedAt = LocalDateTime.now(); }
}

// ── PartyCategory (1차 카테고리) ───────────────────────────────────────────────
@Entity @Table(name = "party_category")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartyCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String name;
    @Column(length = 200) private String description;
    @Column(length = 10) private String icon;
    @Column(name = "sort_order", nullable = false) @Builder.Default private int sortOrder = 0;
    @Column(name = "is_active", nullable = false) @Builder.Default private boolean isActive = true;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

// ── PartySubcategory (2차 카테고리) ───────────────────────────────────────────
@Entity @Table(name = "party_subcategory")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartySubcategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "category_id", nullable = false) private Long categoryId;
    @Column(nullable = false, length = 50) private String name;
    @Column(length = 200) private String description;
    @Column(name = "min_level") private Integer minLevel;
    @Column(name = "sort_order", nullable = false) @Builder.Default private int sortOrder = 0;
    @Column(name = "is_active", nullable = false) @Builder.Default private boolean isActive = true;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

// ── PartyPost (파티 모집 글) ───────────────────────────────────────────────────
@Entity @Table(name = "party_post")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartyPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "category_id", nullable = false) private Long categoryId;
    @Column(name = "subcategory_id", nullable = false) private Long subcategoryId;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "character_id") private Long characterId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false, length = 50) private String server;
    @Column(name = "min_level") private Integer minLevel;
    @Column(name = "max_members", nullable = false) @Builder.Default private int maxMembers = 4;
    @Column(name = "current_members", nullable = false) @Builder.Default private int currentMembers = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default
    private Status status = Status.OPEN;
    @Column(name = "schedule_time") private LocalDateTime scheduleTime;
    @Column(nullable = false) @Builder.Default private int views = 0;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void onUpdate() { this.updatedAt = LocalDateTime.now(); }
    public enum Status { OPEN, CLOSED, DELETED }
}

// ── PartyComment (댓글) ───────────────────────────────────────────────────────
@Entity @Table(name = "party_comment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartyComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "is_deleted", nullable = false) @Builder.Default private boolean isDeleted = false;
    @Column(nullable = false, updatable = false) @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
