package com.aion2.dto;

import com.aion2.entity.*;
import lombok.*;
import java.time.*;
import java.util.List;

public class AuthDto {
    @Getter @Setter public static class LoginRequest { private String username; private String password; }
    @Getter @Setter public static class SignupRequest { private String username; private String password; private String nickname; }
    @Getter @Builder public static class LoginResponse {
        private String token; private Long id; private String username;
        private String nickname; private String role; private String authProvider; private String profileImage;
    }
}

public class AccountDto {
    @Getter @Setter public static class CreateRequest { private String username; private String password; }
    @Getter @Builder public static class Summary {
        private Long id; private String username; private String nickname;
        private String role; private String authProvider; private LocalDateTime createdAt;
        public static Summary from(Account a) {
            return Summary.builder().id(a.getId()).username(a.getUsername()).nickname(a.getNickname())
                .role(a.getRole().name()).authProvider(a.getAuthProvider().name()).createdAt(a.getCreatedAt()).build();
        }
    }
}

public class CharacterDto {
    @Getter @Setter public static class AddRequest { private String server; private String name; }
    @Getter @Builder public static class Detail {
        private Long id; private String server; private String name; private String className;
        private int level; private long combatPower; private String grade; private String gradeColor;
        private String wings; private long honor; private boolean verified;
        private LocalDateTime verifiedAt; private LocalDateTime fetchedAt; private LocalDateTime createdAt;
        public static Detail from(Character c) {
            return Detail.builder().id(c.getId()).server(c.getServer()).name(c.getName())
                .className(c.getClassName()).level(c.getLevel()).combatPower(c.getCombatPower())
                .grade(c.getGrade()).gradeColor(c.getGradeColor()).wings(c.getWings())
                .honor(c.getHonor()).verified(c.isVerified()).verifiedAt(c.getVerifiedAt())
                .fetchedAt(c.getFetchedAt()).createdAt(c.getCreatedAt()).build();
        }
    }
}

public class QuestDto {
    @Getter @Setter public static class CreateRequest {
        private String name; private String type; private Integer minLevel; private Integer maxLevel;
        private String days; private int resetDay = 3; private int resetHour = 5;
    }
    @Getter @Builder public static class Summary {
        private Long id; private String name; private String type;
        private Integer minLevel; private Integer maxLevel; private int resetDay; private int resetHour; private String resetInfo;
        public static Summary from(QuestTemplate q) {
            String[] days = {"","월","화","수","목","금","토","일"};
            String ri = q.getType() == QuestTemplate.QuestType.DAILY
                ? "매일 오전 " + q.getResetHour() + "시"
                : "매주 " + (q.getResetDay()>=1&&q.getResetDay()<=7?days[q.getResetDay()]:"?") + "요일 오전 " + q.getResetHour() + "시";
            return Summary.builder().id(q.getId()).name(q.getName()).type(q.getType().name())
                .minLevel(q.getMinLevel()).maxLevel(q.getMaxLevel())
                .resetDay(q.getResetDay()).resetHour(q.getResetHour()).resetInfo(ri).build();
        }
    }
    @Getter @Builder public static class QuestStatus {
        private Long questId; private String name; private String type;
        private boolean done; private String resetInfo; private LocalDate dateKey;
    }
    @Getter @Builder public static class CharacterDashboard {
        private Long characterId; private String characterName; private String server;
        private int level; private boolean verified; private List<QuestStatus> quests;
        private int totalCount; private int doneCount;
    }
}

public class VerificationDto {
    @Getter @Builder public static class IssueResponse {
        private String code; private String articleUrl; private LocalDateTime expiresAt; private String characterName;
    }
    @Getter @Builder public static class VerifyResponse { private boolean success; private String message; }
}

public class PostDto {
    @Getter @Builder public static class Summary {
        private Long id; private String title; private String content;
        private String sourceUrl; private String category; private LocalDateTime createdAt;
        public static Summary from(Post p) {
            return Summary.builder().id(p.getId()).title(p.getTitle()).content(p.getContent())
                .sourceUrl(p.getSourceUrl()).category(p.getCategory().name()).createdAt(p.getCreatedAt()).build();
        }
    }
}

@Getter @Builder public class RankingDto {
    private int rank; private String displayName; private String className;
    private int level; private long combatPower; private String grade; private boolean verified;
}
@Getter @Builder public class StatsDto { private long totalUsers; private long totalCharacters; }

// ── 파티 모집 게시판 DTO ───────────────────────────────────────────────────────
public class PartyCategoryDto {
    @Getter @Setter public static class CreateRequest {
        private String name; private String description; private String icon; private int sortOrder;
    }
    @Getter @Builder public static class Response {
        private Long id; private String name; private String description;
        private String icon; private int sortOrder; private boolean isActive; private LocalDateTime createdAt;
        public static Response from(PartyCategory c) {
            return Response.builder().id(c.getId()).name(c.getName()).description(c.getDescription())
                .icon(c.getIcon()).sortOrder(c.getSortOrder()).isActive(c.isActive()).createdAt(c.getCreatedAt()).build();
        }
    }
    @Getter @Builder public static class WithSubs {
        private Long id; private String name; private String description; private String icon;
        private int sortOrder; private List<PartySubcategoryDto.Response> subcategories;
        public static WithSubs from(PartyCategory c, List<PartySubcategoryDto.Response> subs) {
            return WithSubs.builder().id(c.getId()).name(c.getName()).description(c.getDescription())
                .icon(c.getIcon()).sortOrder(c.getSortOrder()).subcategories(subs).build();
        }
    }
}

public class PartySubcategoryDto {
    @Getter @Setter public static class CreateRequest {
        private Long categoryId; private String name; private String description; private Integer minLevel; private int sortOrder;
    }
    @Getter @Builder public static class Response {
        private Long id; private Long categoryId; private String name; private String description;
        private Integer minLevel; private int sortOrder; private boolean isActive;
        public static Response from(PartySubcategory s) {
            return Response.builder().id(s.getId()).categoryId(s.getCategoryId()).name(s.getName())
                .description(s.getDescription()).minLevel(s.getMinLevel())
                .sortOrder(s.getSortOrder()).isActive(s.isActive()).build();
        }
    }
}

public class PartyPostDto {
    @Getter @Setter public static class CreateRequest {
        private Long categoryId; private Long subcategoryId; private Long characterId;
        private String title; private String content; private String server;
        private Integer minLevel; private int maxMembers; private LocalDateTime scheduleTime;
    }
    @Getter @Builder public static class Summary {
        private Long id; private Long categoryId; private String categoryName;
        private Long subcategoryId; private String subcategoryName;
        private String title; private String server; private Integer minLevel;
        private int maxMembers; private int currentMembers; private String status;
        private int views; private long commentCount; private String authorName;
        private String authorProvider; private LocalDateTime createdAt;
    }
    @Getter @Builder public static class Detail {
        private Long id; private Long categoryId; private String categoryName;
        private Long subcategoryId; private String subcategoryName;
        private Long accountId; private String authorName; private String authorProvider;
        private String authorProfileImage; private Long characterId; private String characterName;
        private String title; private String content; private String server;
        private Integer minLevel; private int maxMembers; private int currentMembers;
        private String status; private LocalDateTime scheduleTime;
        private int views; private LocalDateTime createdAt; private LocalDateTime updatedAt;
        private List<CommentResponse> comments;
    }
    @Getter @Builder public static class CommentResponse {
        private Long id; private Long accountId; private String authorName;
        private String authorProvider; private String content; private LocalDateTime createdAt;
    }
}
