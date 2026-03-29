package com.aion2.service;

import com.aion2.dto.AccountDto;
import com.aion2.dto.QuestDto;
import com.aion2.entity.Account;
import com.aion2.entity.QuestTemplate;
import com.aion2.entity.SystemConfig;
import com.aion2.repository.AccountRepository;
import com.aion2.repository.QuestTemplateRepository;
import com.aion2.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AccountRepository accountRepo;
    private final QuestTemplateRepository questRepo;
    private final SystemConfigRepository configRepo;
    private final PasswordEncoder passwordEncoder;

    // ── 계정 관리 ─────────────────────────────────────────────────────────────
    public List<AccountDto.Summary> findAllAccounts() {
        return accountRepo.findAll().stream().map(AccountDto.Summary::from).toList();
    }

    public AccountDto.Summary createAccount(AccountDto.CreateRequest req) {
        if (accountRepo.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다");
        }
        Account account = Account.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .build();
        return AccountDto.Summary.from(accountRepo.save(account));
    }

    // ── 퀘스트 관리 ───────────────────────────────────────────────────────────
    public List<QuestDto.Summary> findAllQuests() {
        return questRepo.findByIsActiveTrueOrderByTypeAscNameAsc()
            .stream().map(QuestDto.Summary::from).toList();
    }

    /**
     * 퀘스트 템플릿 생성
     * reset_day:  1=월 ~ 7=일 (주간 초기화 요일, 기본 3=수)
     * reset_hour: 0~23 (초기화 시각, 기본 5=오전5시)
     */
    public QuestDto.Summary createQuest(QuestDto.CreateRequest req, Long adminId) {
        // 유효성 검사
        if (req.getResetDay() < 1 || req.getResetDay() > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "reset_day는 1(월)~7(일) 사이여야 합니다");
        }
        if (req.getResetHour() < 0 || req.getResetHour() > 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "reset_hour는 0~23 사이여야 합니다");
        }

        QuestTemplate qt = QuestTemplate.builder()
            .name(req.getName())
            .type(QuestTemplate.QuestType.valueOf(req.getType()))
            .minLevel(req.getMinLevel())
            .maxLevel(req.getMaxLevel())
            .days(req.getDays())
            .resetDay(req.getResetDay())
            .resetHour(req.getResetHour())
            .createdBy(adminId)
            .build();

        return QuestDto.Summary.from(questRepo.save(qt));
    }

    // ── 인증 설정 관리 ────────────────────────────────────────────────────────
    public Map<String, String> getVerifyConfig() {
        return Map.of(
            "article_url",         configRepo.getValue("verify.article_url", ""),
            "code_expire_minutes", configRepo.getValue("verify.code_expire_minutes", "10"),
            "code_prefix",         configRepo.getValue("verify.code_prefix", "AION")
        );
    }

    public void updateVerifyConfig(Map<String, String> config) {
        config.forEach((k, v) -> {
            SystemConfig sc = new SystemConfig();
            sc.setConfigKey("verify." + k);
            sc.setConfigValue(v);
            configRepo.save(sc);
        });
    }
}
