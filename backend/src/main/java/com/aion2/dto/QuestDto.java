package com.aion2.dto;

import com.aion2.entity.QuestTemplate;
import lombok.*;
import java.time.LocalDate;

/**
 * 퀘스트 관련 DTO — reset_day, reset_hour 포함
 */
public class QuestDto {

    /** 관리자 퀘스트 생성 요청 */
    @Getter
    @Setter
    public static class CreateRequest {
        private String name;
        private String type;         // DAILY | WEEKLY | SPECIFIC
        private Integer minLevel;
        private Integer maxLevel;
        private String days;
        private int resetDay  = 3;   // 기본: 수요일
        private int resetHour = 5;   // 기본: 오전 5시
    }

    /** 퀘스트 템플릿 요약 (목록 표시용) */
    @Getter
    @Builder
    public static class Summary {
        private Long id;
        private String name;
        private String type;
        private Integer minLevel;
        private Integer maxLevel;
        private int resetDay;
        private int resetHour;
        private String resetInfo;    // "매주 수요일 오전 5시 초기화" 같은 안내 문자열

        public static Summary from(QuestTemplate q) {
            String[] days = {"", "월", "화", "수", "목", "금", "토", "일"};
            String resetInfo;
            if (q.getType() == QuestTemplate.QuestType.DAILY) {
                resetInfo = "매일 오전 " + q.getResetHour() + "시 초기화";
            } else if (q.getType() == QuestTemplate.QuestType.WEEKLY) {
                String day = (q.getResetDay() >= 1 && q.getResetDay() <= 7)
                    ? days[q.getResetDay()] : "?";
                resetInfo = "매주 " + day + "요일 오전 " + q.getResetHour() + "시 초기화";
            } else {
                resetInfo = "초기화 없음";
            }
            return Summary.builder()
                .id(q.getId())
                .name(q.getName())
                .type(q.getType().name())
                .minLevel(q.getMinLevel())
                .maxLevel(q.getMaxLevel())
                .resetDay(q.getResetDay())
                .resetHour(q.getResetHour())
                .resetInfo(resetInfo)
                .build();
        }
    }

    /** 대시보드용 개별 퀘스트 상태 */
    @Getter
    @Builder
    public static class QuestStatus {
        private Long questId;
        private String name;
        private String type;
        private boolean done;
        private String resetInfo;    // "매주 수요일 오전 5시 초기화"
        private LocalDate dateKey;   // 현재 초기화 주기 시작일 (표시용)
    }

    /** 대시보드용 캐릭터별 퀘스트 묶음 */
    @Getter
    @Builder
    public static class CharacterDashboard {
        private Long characterId;
        private String characterName;
        private String server;
        private int level;
        private boolean verified;
        private java.util.List<QuestStatus> quests;
        private int totalCount;
        private int doneCount;
    }
}
