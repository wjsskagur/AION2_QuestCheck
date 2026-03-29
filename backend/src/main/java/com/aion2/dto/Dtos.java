package com.aion2.dto;

import com.aion2.entity.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ── Auth ──────────────────────────────────────────────────────────────────────
public class AuthDto {
    @Getter @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Getter @Builder
    public static class LoginResponse {
        private String token;
        private Long id;
        private String username;
        private String role;
    }
}

// ── Account ───────────────────────────────────────────────────────────────────
public class AccountDto {
    @Getter @Setter
    public static class CreateRequest {
        private String username;
        private String password;
    }

    @Getter @Builder
    public static class Summary {
        private Long id;
        private String username;
        private String role;
        private LocalDateTime createdAt;

        public static Summary from(Account a) {
            return Summary.builder()
                .id(a.getId()).username(a.getUsername())
                .role(a.getRole().name()).createdAt(a.getCreatedAt()).build();
        }
    }
}

// ── Character ─────────────────────────────────────────────────────────────────
public class CharacterDto {
    @Getter @Setter
    public static class AddRequest {
        private String server;
        private String name;
    }

    @Getter @Builder
    public static class Detail {
        private Long id;
        private String server;
        private String name;
        private String className;
        private int level;
        private long combatPower;
        private String grade;
        private String gradeColor;
        private String wings;
        private long honor;
        private boolean verified;
        private LocalDateTime verifiedAt;
        private LocalDateTime fetchedAt;
        private LocalDateTime createdAt;

        public static Detail from(Character c) {
            return Detail.builder()
                .id(c.getId()).server(c.getServer()).name(c.getName())
                .className(c.getClassName()).level(c.getLevel())
                .combatPower(c.getCombatPower()).grade(c.getGrade())
                .gradeColor(c.getGradeColor()).wings(c.getWings())
                .honor(c.getHonor()).verified(c.isVerified())
                .verifiedAt(c.getVerifiedAt()).fetchedAt(c.getFetchedAt())
                .createdAt(c.getCreatedAt()).build();
        }
    }
}

// ── Quest ─────────────────────────────────────────────────────────────────────
public class QuestDto {
    @Getter @Setter
    public static class CreateRequest {
        private String name;
        private String type;
        private Integer minLevel;
        private Integer maxLevel;
        private String days;
    }

    @Getter @Builder
    public static class Summary {
        private Long id;
        private String name;
        private String type;
        private Integer minLevel;
        private Integer maxLevel;

        public static Summary from(QuestTemplate q) {
            return Summary.builder()
                .id(q.getId()).name(q.getName()).type(q.getType().name())
                .minLevel(q.getMinLevel()).maxLevel(q.getMaxLevel()).build();
        }
    }

    @Getter @Builder
    public static class QuestStatus {
        private Long questId;
        private String name;
        private String type;
        private boolean done;
        private LocalDate dateKey;
    }

    @Getter @Builder
    public static class CharacterDashboard {
        private Long characterId;
        private String characterName;
        private String server;
        private int level;
        private boolean verified;
        private List<QuestStatus> quests;
        private int totalCount;
        private int doneCount;
    }
}

// ── Verification ──────────────────────────────────────────────────────────────
public class VerificationDto {
    @Getter @Builder
    public static class IssueResponse {
        private String code;
        private String articleUrl;
        private LocalDateTime expiresAt;
        private String characterName;
    }

    @Getter @Builder
    public static class VerifyResponse {
        private boolean success;
        private String message;
    }

    @Getter @Builder
    public static class StatusResponse {
        private boolean verified;
        private LocalDateTime verifiedAt;
        private boolean hasPendingCode;
        private String pendingCode;
        private LocalDateTime expiresAt;
    }
}

// ── Post ──────────────────────────────────────────────────────────────────────
public class PostDto {
    @Getter @Builder
    public static class Summary {
        private Long id;
        private String title;
        private String content;
        private String sourceUrl;
        private String category;
        private LocalDateTime createdAt;

        public static Summary from(Post p) {
            return Summary.builder()
                .id(p.getId()).title(p.getTitle()).content(p.getContent())
                .sourceUrl(p.getSourceUrl()).category(p.getCategory().name())
                .createdAt(p.getCreatedAt()).build();
        }
    }
}

// ── Ranking ───────────────────────────────────────────────────────────────────
@Getter @Builder
public class RankingDto {
    private int rank;
    private String displayName;
    private String className;
    private int level;
    private long combatPower;
    private String grade;
    private boolean verified;
}

// ── Stats ─────────────────────────────────────────────────────────────────────
@Getter @Builder
public class StatsDto {
    private long totalUsers;
    private long totalCharacters;
}
