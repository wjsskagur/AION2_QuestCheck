package com.aion2.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 퀘스트 템플릿
 *
 * reset_day:  초기화 요일 (1=월 2=화 3=수 4=목 5=금 6=토 7=일)
 *             기본값 3 = 수요일 (아이온2 주간 초기화 기준)
 *             DAILY 타입은 이 값을 사용하지 않음
 *
 * reset_hour: 초기화 시각 (0~23), 기본값 5 = 오전 5시
 *             DAILY, WEEKLY 모두 이 값으로 초기화 시각 결정
 */
@Entity
@Table(name = "quest_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /** 주간 초기화 요일: 1=월 ~ 7=일, 기본 3(수요일) */
    @Column(nullable = false)
    @Builder.Default
    private int resetDay = 3;

    /** 초기화 시각: 0~23, 기본 5(오전 5시) */
    @Column(nullable = false)
    @Builder.Default
    private int resetHour = 5;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum QuestType {
        DAILY,    // 매일 reset_hour 시각에 초기화
        WEEKLY,   // 매주 reset_day요일 reset_hour 시각에 초기화
        SPECIFIC  // 특정 날짜 (date_key = 당일 00:00, 수동 관리)
    }
}
