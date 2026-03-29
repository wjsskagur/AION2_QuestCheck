package com.aion2.service;

import com.aion2.dto.QuestDto;
import com.aion2.entity.Character;
import com.aion2.entity.QuestCheck;
import com.aion2.entity.QuestTemplate;
import com.aion2.repository.CharacterRepository;
import com.aion2.repository.QuestCheckRepository;
import com.aion2.repository.QuestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 퀘스트 초기화 시점 계산 로직
 *
 * quest_template.reset_day  (1=월 ~ 7=일): 주간 퀘스트 초기화 요일
 * quest_template.reset_hour (0~23)        : 초기화 시각 (일일/주간 공통)
 *
 * 예) reset_day=3(수), reset_hour=5(오전5시) → 수요일 오전 5시마다 초기화
 *
 * 핵심 아이디어:
 *   "현재 시각이 속한 초기화 주기의 시작 시점(= 가장 최근 초기화 시점)"을
 *   date_key(DATETIME)로 저장한다.
 *
 *   [일일] 오늘 reset_hour 이전이면 → 어제 reset_hour 시각을 date_key로 사용
 *         오늘 reset_hour 이후이면 → 오늘 reset_hour 시각을 date_key로 사용
 *
 *   [주간] 이번 주 reset_day의 reset_hour 시각을 기준으로,
 *          현재 시각이 그 기준보다 이전이면 → 지난 주 reset_day의 reset_hour
 *          현재 시각이 그 기준보다 이후이면 → 이번 주 reset_day의 reset_hour
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuestCheckService {

    private final QuestCheckRepository checkRepo;
    private final QuestTemplateRepository templateRepo;
    private final CharacterRepository charRepo;

    // ── 대시보드 조회 ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<QuestDto.CharacterDashboard> getDashboard(Long accountId) {
        return charRepo.findByAccountIdOrderByCreatedAtDesc(accountId)
            .stream()
            .map(ch -> buildDashboard(ch))
            .toList();
    }

    private QuestDto.CharacterDashboard buildDashboard(Character ch) {
        List<QuestTemplate> templates = templateRepo.findApplicable(ch.getLevel());
        LocalDateTime now = LocalDateTime.now();

        List<QuestDto.QuestStatus> statuses = templates.stream().map(qt -> {
            LocalDateTime dateKey = calcDateKey(qt, now);
            boolean done = checkRepo.existsByCharacterIdAndQuestIdAndDateKey(
                ch.getId(), qt.getId(), dateKey);
            return QuestDto.QuestStatus.builder()
                .questId(qt.getId())
                .name(qt.getName())
                .type(qt.getType().name())
                .done(done)
                .resetInfo(buildResetInfo(qt))
                .dateKey(dateKey.toLocalDate())
                .build();
        }).toList();

        long doneCount = statuses.stream().filter(QuestDto.QuestStatus::isDone).count();
        return QuestDto.CharacterDashboard.builder()
            .characterId(ch.getId())
            .characterName(ch.getName())
            .server(ch.getServer())
            .level(ch.getLevel())
            .verified(ch.isVerified())
            .quests(statuses)
            .totalCount(statuses.size())
            .doneCount((int) doneCount)
            .build();
    }

    // ── 퀘스트 체크 토글 ──────────────────────────────────────────────────────
    public void toggle(Long accountId, Long charId, Long questId) {
        charRepo.findByIdAndAccountId(charId, accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        QuestTemplate qt = templateRepo.findById(questId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDateTime dateKey = calcDateKey(qt, LocalDateTime.now());

        checkRepo.findByCharacterIdAndQuestIdAndDateKey(charId, questId, dateKey)
            .ifPresentOrElse(
                checkRepo::delete,
                () -> checkRepo.save(QuestCheck.builder()
                    .characterId(charId)
                    .questId(questId)
                    .dateKey(dateKey)
                    .build())
            );
    }

    // ── 핵심: 퀘스트별 초기화 기준 시점(date_key) 계산 ──────────────────────
    /**
     * 현재 시각(now) 기준으로 "이 퀘스트가 속한 초기화 주기의 시작 DATETIME"을 반환.
     * 이 값이 quest_check.date_key 에 저장된다.
     *
     * 같은 초기화 주기 안에 있으면 date_key가 동일 → 체크 상태 유지
     * 다음 초기화 시점이 지나면 date_key가 바뀜 → 자동 리셋 효과
     */
    public static LocalDateTime calcDateKey(QuestTemplate qt, LocalDateTime now) {
        int resetHour = qt.getResetHour(); // 0~23

        if (qt.getType() == QuestTemplate.QuestType.DAILY) {
            return calcDailyDateKey(now, resetHour);
        } else if (qt.getType() == QuestTemplate.QuestType.WEEKLY) {
            int resetDayOfWeek = qt.getResetDay(); // 1=월 ~ 7=일
            return calcWeeklyDateKey(now, resetDayOfWeek, resetHour);
        } else {
            // SPECIFIC: date_key = 오늘 날짜 00:00 (별도 초기화 없음)
            return now.toLocalDate().atStartOfDay();
        }
    }

    /**
     * 일일 퀘스트: 오늘 reset_hour 이전이면 어제 reset_hour, 이후면 오늘 reset_hour
     *
     * 예) reset_hour=5, 현재 2024-01-10 03:00
     *   → 오늘 5시(2024-01-10 05:00) 이전이므로 → 2024-01-09 05:00 반환
     *
     * 예) reset_hour=5, 현재 2024-01-10 07:00
     *   → 오늘 5시(2024-01-10 05:00) 이후이므로 → 2024-01-10 05:00 반환
     */
    private static LocalDateTime calcDailyDateKey(LocalDateTime now, int resetHour) {
        LocalDateTime todayReset = now.toLocalDate().atTime(resetHour, 0, 0);
        if (now.isBefore(todayReset)) {
            // 아직 오늘 초기화 시간 전 → 어제 초기화 시점이 현재 주기
            return todayReset.minusDays(1);
        }
        return todayReset;
    }

    /**
     * 주간 퀘스트: 이번 주 reset_day의 reset_hour 시각을 기준으로,
     *             현재가 그 이전이면 지난 주, 이후면 이번 주 기준 시점 반환
     *
     * 예) reset_day=3(수), reset_hour=5, 현재 2024-01-10(수) 03:00
     *   → 이번 주 수요일 5시(2024-01-10 05:00) 이전 → 지난 주 수요일 5시 반환
     *
     * 예) reset_day=3(수), reset_hour=5, 현재 2024-01-10(수) 07:00
     *   → 이번 주 수요일 5시 이후 → 이번 주 수요일 5시 반환
     */
    private static LocalDateTime calcWeeklyDateKey(LocalDateTime now, int resetDayOfWeek, int resetHour) {
        // Java DayOfWeek: MONDAY=1 ~ SUNDAY=7 (ISO 기준, reset_day와 동일)
        DayOfWeek targetDow = DayOfWeek.of(resetDayOfWeek);

        // 이번 주 target 요일의 날짜 계산
        LocalDate thisWeekTarget = now.toLocalDate()
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(targetDow));

        // 이번 주 초기화 기준 DATETIME
        LocalDateTime thisWeekReset = thisWeekTarget.atTime(resetHour, 0, 0);

        if (now.isBefore(thisWeekReset)) {
            // 아직 이번 주 초기화 시점 이전 → 지난 주 초기화 시점이 현재 주기
            return thisWeekReset.minusWeeks(1);
        }
        return thisWeekReset;
    }

    // ── 다음 초기화 시각 안내 문자열 (프론트 표시용) ─────────────────────────
    private static String buildResetInfo(QuestTemplate qt) {
        String[] days = {"", "월", "화", "수", "목", "금", "토", "일"};
        if (qt.getType() == QuestTemplate.QuestType.DAILY) {
            return "매일 오전 " + qt.getResetHour() + "시 초기화";
        } else if (qt.getType() == QuestTemplate.QuestType.WEEKLY) {
            String day = (qt.getResetDay() >= 1 && qt.getResetDay() <= 7)
                ? days[qt.getResetDay()] : "?";
            return "매주 " + day + "요일 오전 " + qt.getResetHour() + "시 초기화";
        }
        return "";
    }
}
