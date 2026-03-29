package com.aion2.service;

import com.aion2.dto.*;
import com.aion2.entity.*;
import com.aion2.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.HttpStatus.*;

// ── AuthService ───────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final com.aion2.security.JwtProvider jwtProvider;

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Account account = accountRepo.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(req.getPassword(), account.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다");
        }

        return AuthDto.LoginResponse.builder()
            .token(jwtProvider.generate(account))
            .id(account.getId())
            .username(account.getUsername())
            .role(account.getRole().name())
            .build();
    }
}

// ── AdminService ──────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final AccountRepository accountRepo;
    private final QuestTemplateRepository questRepo;
    private final SystemConfigRepository configRepo;
    private final PasswordEncoder passwordEncoder;

    public List<AccountDto.Summary> findAllAccounts() {
        return accountRepo.findAll().stream().map(AccountDto.Summary::from).toList();
    }

    public AccountDto.Summary createAccount(AccountDto.CreateRequest req) {
        if (accountRepo.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 아이디입니다");
        }
        Account account = Account.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .build();
        return AccountDto.Summary.from(accountRepo.save(account));
    }

    public QuestDto.Summary createQuest(QuestDto.CreateRequest req, Long adminId) {
        QuestTemplate qt = QuestTemplate.builder()
            .name(req.getName())
            .type(QuestTemplate.QuestType.valueOf(req.getType()))
            .minLevel(req.getMinLevel())
            .maxLevel(req.getMaxLevel())
            .days(req.getDays())
            .createdBy(adminId)
            .build();
        return QuestDto.Summary.from(questRepo.save(qt));
    }

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

    public List<QuestDto.Summary> findAllQuests() {
        return questRepo.findByIsActiveTrueOrderByTypeAscNameAsc()
            .stream().map(QuestDto.Summary::from).toList();
    }
}

// ── CharacterService ──────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CharacterService {

    @Value("${app.character.max-per-account}")
    private int maxPerAccount;

    private final CharacterRepository charRepo;

    @Transactional(readOnly = true)
    public List<CharacterDto.Detail> findByAccountId(Long accountId) {
        return charRepo.findByAccountIdOrderByCreatedAtDesc(accountId)
            .stream().map(CharacterDto.Detail::from).toList();
    }

    public CharacterDto.Detail addCharacter(Long accountId, CharacterDto.AddRequest req) {
        if (charRepo.countByAccountId(accountId) >= maxPerAccount) {
            throw new ResponseStatusException(BAD_REQUEST,
                "캐릭터는 최대 " + maxPerAccount + "개까지 등록 가능합니다");
        }
        Character character = Character.builder()
            .accountId(accountId)
            .server(req.getServer())
            .name(req.getName())
            .build();
        return CharacterDto.Detail.from(charRepo.save(character));
    }

    public void deleteCharacter(Long accountId, Long characterId) {
        Character character = charRepo.findByIdAndAccountId(characterId, accountId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        charRepo.delete(character);
    }

    // 캐릭터 정보 갱신 — 아이온2 공식 API 연동 시 이 메서드를 확장
    public CharacterDto.Detail refreshCharacter(Long accountId, Long characterId) {
        Character character = charRepo.findByIdAndAccountId(characterId, accountId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        // TODO: 아이온2 공식 캐릭터 정보 API가 공개되면 여기서 호출
        character.setFetchedAt(LocalDateTime.now());
        return CharacterDto.Detail.from(charRepo.save(character));
    }
}

// ── QuestCheckService ─────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
public class QuestCheckService {

    private final QuestCheckRepository checkRepo;
    private final QuestTemplateRepository templateRepo;
    private final CharacterRepository charRepo;

    @Transactional(readOnly = true)
    public List<QuestDto.CharacterDashboard> getDashboard(Long accountId) {
        List<Character> chars = charRepo.findByAccountIdOrderByCreatedAtDesc(accountId);
        return chars.stream().map(ch -> buildDashboard(ch)).toList();
    }

    private QuestDto.CharacterDashboard buildDashboard(Character ch) {
        List<QuestTemplate> templates = templateRepo.findApplicable(ch.getLevel());
        LocalDate today = LocalDate.now();

        List<QuestDto.QuestStatus> statuses = templates.stream().map(qt -> {
            LocalDate dateKey = qt.getType() == QuestTemplate.QuestType.WEEKLY
                ? today.with(DayOfWeek.MONDAY)
                : today;
            boolean done = checkRepo.existsByCharacterIdAndQuestIdAndDateKey(
                ch.getId(), qt.getId(), dateKey);
            return QuestDto.QuestStatus.builder()
                .questId(qt.getId()).name(qt.getName())
                .type(qt.getType().name()).done(done).dateKey(dateKey)
                .build();
        }).toList();

        long doneCount = statuses.stream().filter(QuestDto.QuestStatus::isDone).count();
        return QuestDto.CharacterDashboard.builder()
            .characterId(ch.getId()).characterName(ch.getName())
            .server(ch.getServer()).level(ch.getLevel()).verified(ch.isVerified())
            .quests(statuses).totalCount(statuses.size()).doneCount((int) doneCount)
            .build();
    }

    public void toggle(Long accountId, Long charId, Long questId) {
        charRepo.findByIdAndAccountId(charId, accountId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        QuestTemplate qt = templateRepo.findById(questId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        LocalDate dateKey = qt.getType() == QuestTemplate.QuestType.WEEKLY
            ? LocalDate.now().with(DayOfWeek.MONDAY)
            : LocalDate.now();

        checkRepo.findByCharacterIdAndQuestIdAndDateKey(charId, questId, dateKey)
            .ifPresentOrElse(
                checkRepo::delete,
                () -> checkRepo.save(QuestCheck.builder()
                    .characterId(charId).questId(questId).dateKey(dateKey).build())
            );
    }
}

// ── VerificationService ───────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VerificationService {

    private final VerificationCodeRepository codeRepo;
    private final CharacterRepository charRepo;
    private final SystemConfigRepository configRepo;
    private final CrawlerService crawlerService;

    public VerificationDto.IssueResponse issueCode(Long accountId, Long charId) {
        Character ch = charRepo.findByIdAndAccountId(charId, accountId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (ch.isVerified()) {
            throw new ResponseStatusException(CONFLICT, "이미 인증된 캐릭터입니다");
        }
        codeRepo.expirePendingByCharacterId(charId);

        String prefix = configRepo.getValue("verify.code_prefix", "AION");
        int expMin = Integer.parseInt(configRepo.getValue("verify.code_expire_minutes", "10"));
        String articleUrl = configRepo.getValue("verify.article_url", "");
        String code = prefix + "-" + generateCode();

        codeRepo.save(VerificationCode.builder()
            .accountId(accountId).characterId(charId).code(code)
            .expiresAt(LocalDateTime.now().plusMinutes(expMin)).build());

        return VerificationDto.IssueResponse.builder()
            .code(code).articleUrl(articleUrl)
            .expiresAt(LocalDateTime.now().plusMinutes(expMin))
            .characterName(ch.getName()).build();
    }

    public VerificationDto.VerifyResponse verify(Long accountId, Long charId) {
        VerificationCode vc = codeRepo
            .findTopByCharacterIdAndStatusOrderByCreatedAtDesc(charId, VerificationCode.Status.PENDING)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "발급된 코드가 없습니다"));

        if (vc.isExpired()) {
            vc.setStatus(VerificationCode.Status.EXPIRED);
            throw new ResponseStatusException(BAD_REQUEST, "코드가 만료되었습니다. 다시 발급해주세요");
        }

        String articleUrl = configRepo.getValue("verify.article_url", "");
        CrawlerService.CrawlResult result = crawlerService.findCodeInComments(articleUrl, vc.getCode());

        if (!result.found()) {
            return VerificationDto.VerifyResponse.builder()
                .success(false)
                .message("댓글에서 코드를 찾지 못했습니다. 잠시 후 다시 시도해주세요")
                .build();
        }

        vc.setStatus(VerificationCode.Status.VERIFIED);
        vc.setVerifiedAt(LocalDateTime.now());
        Character ch = charRepo.findById(charId).orElseThrow();
        ch.setVerified(true);
        ch.setVerifiedAt(LocalDateTime.now());

        return VerificationDto.VerifyResponse.builder()
            .success(true).message("인증이 완료되었습니다!").build();
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        return IntStream.range(0, 5)
            .mapToObj(i -> String.valueOf(chars.charAt(
                ThreadLocalRandom.current().nextInt(chars.length()))))
            .collect(Collectors.joining());
    }
}

// ── CrawlerService ────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final PostRepository postRepo;
    private final ClaudeApiService claudeApiService;
    private final SystemConfigRepository configRepo;

    // 공홈 댓글에서 인증 코드 검색
    public CrawlResult findCodeInComments(String articleUrl, String code) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .timeout(15_000)
                .get();

            // 아이온2 출시 후 실제 HTML 구조에 맞게 selector 수정 필요
            Elements comments = doc.select(".comment-item, .reply-item, [class*='comment']");
            for (var comment : comments) {
                if (comment.text().contains(code)) {
                    log.info("인증 코드 발견: {}", code);
                    return new CrawlResult(true);
                }
            }
            return new CrawlResult(false);
        } catch (Exception e) {
            log.error("댓글 크롤링 실패: url={} err={}", articleUrl, e.getMessage());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                "인증 서버 오류. 잠시 후 다시 시도해주세요");
        }
    }

    // 공지 목록 크롤링 + Claude API 요약 (3시간마다 자동 실행)
    @Scheduled(cron = "${app.crawler.schedule}")
    @Transactional
    public void crawlAndSummarize() {
        String noticeUrl = configRepo.getValue("crawler.notice_url", "");
        if (noticeUrl.isBlank()) return;
        try {
            Document doc = Jsoup.connect(noticeUrl)
                .userAgent("Mozilla/5.0").timeout(10_000).get();

            doc.select(".board-list .item, .notice-list li").forEach(el -> {
                String title = el.select(".title, a").first() != null
                    ? el.select(".title, a").first().text() : "";
                String href  = el.select("a").attr("href");
                if (title.isBlank() || href.isBlank()) return;

                String url = href.startsWith("http") ? href : "https://aion2.plaync.com" + href;
                if (postRepo.existsBySourceUrl(url)) return;

                try {
                    String body    = crawlBody(url);
                    String summary = claudeApiService.summarize(title, body);
                    postRepo.save(Post.builder()
                        .title(title).content(summary).sourceUrl(url)
                        .category(detectCategory(title)).createdBy(1L).build());
                    log.info("공지 저장: {}", title);
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    log.warn("공지 개별 처리 실패: {} - {}", title, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("공지 크롤링 실패: {}", e.getMessage());
        }
    }

    private String crawlBody(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10_000).get();
            return doc.select(".article-content, .board-content, .view-content").text();
        } catch (Exception e) {
            return "";
        }
    }

    private Post.Category detectCategory(String title) {
        if (title.contains("업데이트") || title.contains("패치")) return Post.Category.UPDATE;
        if (title.contains("이벤트") || title.contains("EVENT"))  return Post.Category.EVENT;
        return Post.Category.NOTICE;
    }

    public record CrawlResult(boolean found) {}
}

// ── ClaudeApiService ──────────────────────────────────────────────────────────
@Service
@Slf4j
public class ClaudeApiService {

    @Value("${app.claude.api-key}")
    private String apiKey;

    @Value("${app.claude.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String summarize(String title, String bodyText) {
        if (apiKey == null || apiKey.isBlank()) {
            return title + " — 자세한 내용은 원문을 확인해주세요.";
        }
        String truncated = bodyText.length() > 3000
            ? bodyText.substring(0, 3000) + "..."
            : bodyText;

        String prompt = String.format("""
            아래는 아이온2 게임의 공식 공지입니다.
            제목: %s
            내용: %s
            
            다음 규칙으로 한국어 요약을 작성하세요:
            - 3줄 이내 핵심 내용만
            - 날짜·기간 정보는 반드시 포함
            - 게임 용어는 원문 그대로
            - 안내 문구 없이 요약만 작성
            """, title, truncated);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", 500,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.anthropic.com/v1/messages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content =
                (List<Map<String, Object>>) response.getBody().get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            return title + " — 자세한 내용은 원문을 확인해주세요.";
        }
    }
}
