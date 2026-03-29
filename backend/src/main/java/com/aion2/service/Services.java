package com.aion2.service;

import com.aion2.dto.*;
import com.aion2.entity.*;
import com.aion2.repository.*;
import com.aion2.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.util.stream.*;

import static org.springframework.http.HttpStatus.*;

// ── AuthService ───────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional
public class AuthService {
    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Account a = accountRepo.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"));
        if (a.getAuthProvider() != Account.AuthProvider.LOCAL && a.getAuthProvider() != Account.AuthProvider.ADMIN)
            throw new ResponseStatusException(BAD_REQUEST, a.getAuthProvider().name() + " 로그인을 이용해주세요");
        if (!passwordEncoder.matches(req.getPassword(), a.getPassword()))
            throw new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다");
        return buildResponse(a);
    }

    public AuthDto.LoginResponse signup(AuthDto.SignupRequest req) {
        if (accountRepo.existsByUsername(req.getUsername()))
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 아이디입니다");
        return buildResponse(accountRepo.save(Account.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .nickname(req.getNickname())
            .authProvider(Account.AuthProvider.LOCAL).build()));
    }

    public AuthDto.LoginResponse getMe(Account a) { return buildResponse(a); }

    private AuthDto.LoginResponse buildResponse(Account a) {
        return AuthDto.LoginResponse.builder().token(jwtProvider.generate(a))
            .id(a.getId()).username(a.getUsername()).nickname(a.getDisplayName())
            .role(a.getRole().name()).authProvider(a.getAuthProvider().name())
            .profileImage(a.getProfileImage()).build();
    }
}

// ── AdminService ──────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional
public class AdminService {
    private final AccountRepository accountRepo;
    private final QuestTemplateRepository questRepo;
    private final SystemConfigRepository configRepo;
    private final PasswordEncoder passwordEncoder;

    public List<AccountDto.Summary> findAllAccounts() {
        return accountRepo.findAll().stream().map(AccountDto.Summary::from).toList();
    }
    public AccountDto.Summary createAccount(AccountDto.CreateRequest req) {
        if (accountRepo.existsByUsername(req.getUsername()))
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 아이디입니다");
        return AccountDto.Summary.from(accountRepo.save(Account.builder()
            .username(req.getUsername()).password(passwordEncoder.encode(req.getPassword()))
            .authProvider(Account.AuthProvider.ADMIN).build()));
    }
    public List<QuestDto.Summary> findAllQuests() {
        return questRepo.findByIsActiveTrueOrderByTypeAscNameAsc().stream().map(QuestDto.Summary::from).toList();
    }
    public QuestDto.Summary createQuest(QuestDto.CreateRequest req, Long adminId) {
        return QuestDto.Summary.from(questRepo.save(QuestTemplate.builder()
            .name(req.getName()).type(QuestTemplate.QuestType.valueOf(req.getType()))
            .minLevel(req.getMinLevel()).maxLevel(req.getMaxLevel())
            .resetDay(req.getResetDay()).resetHour(req.getResetHour()).createdBy(adminId).build()));
    }
    public Map<String, String> getVerifyConfig() {
        return Map.of("article_url", configRepo.getValue("verify.article_url",""),
            "code_expire_minutes", configRepo.getValue("verify.code_expire_minutes","10"),
            "code_prefix", configRepo.getValue("verify.code_prefix","AION"));
    }
    public void updateVerifyConfig(Map<String, String> config) {
        config.forEach((k, v) -> { SystemConfig sc = new SystemConfig(); sc.setConfigKey("verify."+k); sc.setConfigValue(v); configRepo.save(sc); });
    }
}

// ── CharacterService ──────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional
public class CharacterService {
    @Value("${app.character.max-per-account}") private int maxPerAccount;
    private final CharacterRepository charRepo;

    public List<CharacterDto.Detail> findByAccountId(Long aid) {
        return charRepo.findByAccountIdOrderByCreatedAtDesc(aid).stream().map(CharacterDto.Detail::from).toList();
    }
    public CharacterDto.Detail add(Long aid, CharacterDto.AddRequest req) {
        if (charRepo.countByAccountId(aid) >= maxPerAccount)
            throw new ResponseStatusException(BAD_REQUEST, "최대 " + maxPerAccount + "개까지 등록 가능합니다");
        return CharacterDto.Detail.from(charRepo.save(
            Character.builder().accountId(aid).server(req.getServer()).name(req.getName()).build()));
    }
    public void delete(Long aid, Long cid) {
        charRepo.delete(charRepo.findByIdAndAccountId(cid, aid)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }
    public CharacterDto.Detail refresh(Long aid, Long cid) {
        Character c = charRepo.findByIdAndAccountId(cid, aid)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        c.setFetchedAt(LocalDateTime.now());
        return CharacterDto.Detail.from(charRepo.save(c));
    }
}

// ── QuestCheckService ─────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional
public class QuestCheckService {
    private final QuestCheckRepository checkRepo;
    private final QuestTemplateRepository templateRepo;
    private final CharacterRepository charRepo;

    public List<QuestDto.CharacterDashboard> getDashboard(Long aid) {
        return charRepo.findByAccountIdOrderByCreatedAtDesc(aid).stream()
            .map(ch -> buildDashboard(ch)).toList();
    }

    private QuestDto.CharacterDashboard buildDashboard(Character ch) {
        List<QuestTemplate> templates = templateRepo.findApplicable(ch.getLevel());
        LocalDateTime now = LocalDateTime.now();
        List<QuestDto.QuestStatus> statuses = templates.stream().map(qt -> {
            LocalDateTime dk = calcDateKey(qt, now);
            return QuestDto.QuestStatus.builder().questId(qt.getId()).name(qt.getName())
                .type(qt.getType().name())
                .done(checkRepo.existsByCharacterIdAndQuestIdAndDateKey(ch.getId(), qt.getId(), dk))
                .resetInfo(buildResetInfo(qt)).dateKey(dk.toLocalDate()).build();
        }).toList();
        long done = statuses.stream().filter(QuestDto.QuestStatus::isDone).count();
        return QuestDto.CharacterDashboard.builder()
            .characterId(ch.getId()).characterName(ch.getName()).server(ch.getServer())
            .level(ch.getLevel()).verified(ch.isVerified())
            .quests(statuses).totalCount(statuses.size()).doneCount((int)done).build();
    }

    public void toggle(Long aid, Long cid, Long qid) {
        charRepo.findByIdAndAccountId(cid, aid).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        QuestTemplate qt = templateRepo.findById(qid).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        LocalDateTime dk = calcDateKey(qt, LocalDateTime.now());
        checkRepo.findByCharacterIdAndQuestIdAndDateKey(cid, qid, dk)
            .ifPresentOrElse(checkRepo::delete,
                () -> checkRepo.save(QuestCheck.builder().characterId(cid).questId(qid).dateKey(dk).build()));
    }

    public static LocalDateTime calcDateKey(QuestTemplate qt, LocalDateTime now) {
        int h = qt.getResetHour();
        if (qt.getType() == QuestTemplate.QuestType.DAILY) {
            LocalDateTime t = now.toLocalDate().atTime(h, 0);
            return now.isBefore(t) ? t.minusDays(1) : t;
        } else if (qt.getType() == QuestTemplate.QuestType.WEEKLY) {
            LocalDate target = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.of(qt.getResetDay())));
            LocalDateTime t = target.atTime(h, 0);
            return now.isBefore(t) ? t.minusWeeks(1) : t;
        }
        return now.toLocalDate().atStartOfDay();
    }

    private static String buildResetInfo(QuestTemplate qt) {
        String[] days = {"","월","화","수","목","금","토","일"};
        if (qt.getType() == QuestTemplate.QuestType.DAILY) return "매일 오전 " + qt.getResetHour() + "시 초기화";
        if (qt.getType() == QuestTemplate.QuestType.WEEKLY)
            return "매주 " + (qt.getResetDay()>=1&&qt.getResetDay()<=7?days[qt.getResetDay()]:"?") + "요일 오전 " + qt.getResetHour() + "시 초기화";
        return "";
    }
}

// ── VerificationService ───────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional @Slf4j
public class VerificationService {
    private final VerificationCodeRepository codeRepo;
    private final CharacterRepository charRepo;
    private final SystemConfigRepository configRepo;
    private final CrawlerService crawlerService;

    public VerificationDto.IssueResponse issue(Long aid, Long cid) {
        Character ch = charRepo.findByIdAndAccountId(cid, aid)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (ch.isVerified()) throw new ResponseStatusException(CONFLICT, "이미 인증된 캐릭터입니다");
        codeRepo.expirePendingByCharacterId(cid);
        String prefix = configRepo.getValue("verify.code_prefix", "AION");
        int expMin = Integer.parseInt(configRepo.getValue("verify.code_expire_minutes", "10"));
        String code = prefix + "-" + genCode();
        codeRepo.save(VerificationCode.builder().accountId(aid).characterId(cid).code(code)
            .expiresAt(LocalDateTime.now().plusMinutes(expMin)).build());
        return VerificationDto.IssueResponse.builder().code(code)
            .articleUrl(configRepo.getValue("verify.article_url",""))
            .expiresAt(LocalDateTime.now().plusMinutes(expMin)).characterName(ch.getName()).build();
    }

    public VerificationDto.VerifyResponse verify(Long aid, Long cid) {
        VerificationCode vc = codeRepo.findTopByCharacterIdAndStatusOrderByCreatedAtDesc(cid, VerificationCode.Status.PENDING)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "발급된 코드가 없습니다"));
        if (vc.isExpired()) { vc.setStatus(VerificationCode.Status.EXPIRED); throw new ResponseStatusException(BAD_REQUEST, "코드 만료"); }
        CrawlerService.CrawlResult r = crawlerService.findCodeInComments(configRepo.getValue("verify.article_url",""), vc.getCode());
        if (!r.found()) return VerificationDto.VerifyResponse.builder().success(false).message("댓글에서 코드를 찾지 못했습니다. 잠시 후 다시 시도해주세요").build();
        vc.setStatus(VerificationCode.Status.VERIFIED); vc.setVerifiedAt(LocalDateTime.now());
        Character ch = charRepo.findById(cid).orElseThrow();
        ch.setVerified(true); ch.setVerifiedAt(LocalDateTime.now());
        return VerificationDto.VerifyResponse.builder().success(true).message("인증 완료!").build();
    }

    private String genCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        return IntStream.range(0,5).mapToObj(i->String.valueOf(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())))).collect(Collectors.joining());
    }
}

// ── CrawlerService ────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Slf4j
public class CrawlerService {
    private final PostRepository postRepo;
    private final ClaudeApiService claudeApi;
    private final SystemConfigRepository configRepo;

    public CrawlResult findCodeInComments(String url, String code) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15_000).get();
            return new CrawlResult(doc.select(".comment-item, .reply-item, [class*='comment']").stream()
                .anyMatch(el -> el.text().contains(code)));
        } catch (Exception e) {
            log.error("크롤링 실패: {}", e.getMessage());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "인증 서버 오류");
        }
    }

    @Scheduled(cron = "${app.crawler.schedule}")
    @Transactional
    public void crawlAndSummarize() {
        String url = configRepo.getValue("crawler.notice_url", "");
        if (url.isBlank()) return;
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10_000).get();
            doc.select(".board-list .item, .notice-list li").forEach(el -> {
                String title = el.select(".title, a").first() != null ? el.select(".title, a").first().text() : "";
                String href = el.select("a").attr("href");
                if (title.isBlank() || href.isBlank()) return;
                String postUrl = href.startsWith("http") ? href : "https://aion2.plaync.com" + href;
                if (postRepo.existsBySourceUrl(postUrl)) return;
                try {
                    String body = crawlBody(postUrl);
                    postRepo.save(Post.builder().title(title).content(claudeApi.summarize(title, body))
                        .sourceUrl(postUrl).category(detectCategory(title)).createdBy(1L).build());
                    Thread.sleep(1000);
                } catch (Exception ex) { log.warn("공지 처리 실패: {}", ex.getMessage()); }
            });
        } catch (Exception e) { log.error("공지 크롤링 실패: {}", e.getMessage()); }
    }

    private String crawlBody(String url) {
        try { return Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10_000).get().select(".article-content, .board-content").text(); }
        catch (Exception e) { return ""; }
    }
    private Post.Category detectCategory(String t) {
        if (t.contains("업데이트")||t.contains("패치")) return Post.Category.UPDATE;
        if (t.contains("이벤트")||t.contains("EVENT")) return Post.Category.EVENT;
        return Post.Category.NOTICE;
    }
    public record CrawlResult(boolean found) {}
}

// ── ClaudeApiService ──────────────────────────────────────────────────────────
@Service @Slf4j
public class ClaudeApiService {
    @Value("${app.claude.api-key}") private String apiKey;
    @Value("${app.claude.model}") private String model;
    private final RestTemplate restTemplate = new RestTemplate();

    public String summarize(String title, String body) {
        if (apiKey == null || apiKey.isBlank()) return title + " — 원문을 확인해주세요.";
        String prompt = "아이온2 공지를 한국어 3줄 이내로 요약. 날짜·기간 필수 포함.\n제목: %s\n내용: %s".formatted(
            title, body.length() > 3000 ? body.substring(0, 3000) : body);
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            var resp = restTemplate.exchange("https://api.anthropic.com/v1/messages", HttpMethod.POST,
                new HttpEntity<>(Map.of("model", model, "max_tokens", 500,
                    "messages", List.of(Map.of("role","user","content",prompt))), headers), Map.class);
            @SuppressWarnings("unchecked")
            var content = (List<Map<String, Object>>) resp.getBody().get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) { log.error("Claude API 실패: {}", e.getMessage()); return title + " — 원문을 확인해주세요."; }
    }
}

// ── PartyBoardService ─────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Transactional @Slf4j
public class PartyBoardService {
    private final PartyCategoryRepository catRepo;
    private final PartySubcategoryRepository subRepo;
    private final PartyPostRepository postRepo;
    private final PartyCommentRepository commentRepo;
    private final CharacterRepository charRepo;
    private final AccountRepository accountRepo;

    // ── 카테고리 조회 (공개) ──────────────────────────────────────────────────
    public List<PartyCategoryDto.WithSubs> getAllCategoriesWithSubs() {
        return catRepo.findByIsActiveTrueOrderBySortOrderAscNameAsc().stream()
            .map(cat -> PartyCategoryDto.WithSubs.from(cat,
                subRepo.findByCategoryIdAndIsActiveTrueOrderBySortOrderAscNameAsc(cat.getId())
                    .stream().map(PartySubcategoryDto.Response::from).toList()))
            .toList();
    }

    // ── 1차 카테고리 관리 (관리자) ────────────────────────────────────────────
    public PartyCategoryDto.Response createCategory(PartyCategoryDto.CreateRequest req, Long adminId) {
        if (catRepo.existsByName(req.getName()))
            throw new ResponseStatusException(CONFLICT, "이미 존재하는 카테고리명입니다");
        return PartyCategoryDto.Response.from(catRepo.save(PartyCategory.builder()
            .name(req.getName()).description(req.getDescription()).icon(req.getIcon())
            .sortOrder(req.getSortOrder()).createdBy(adminId).build()));
    }

    public PartyCategoryDto.Response updateCategory(Long id, PartyCategoryDto.CreateRequest req) {
        PartyCategory cat = catRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        cat.setName(req.getName()); cat.setDescription(req.getDescription());
        cat.setIcon(req.getIcon()); cat.setSortOrder(req.getSortOrder());
        return PartyCategoryDto.Response.from(catRepo.save(cat));
    }

    public void deleteCategory(Long id) {
        PartyCategory cat = catRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        cat.setActive(false); // 소프트 삭제
        catRepo.save(cat);
    }

    // ── 2차 카테고리 관리 (관리자) ────────────────────────────────────────────
    public PartySubcategoryDto.Response createSubcategory(PartySubcategoryDto.CreateRequest req, Long adminId) {
        if (!catRepo.existsById(req.getCategoryId()))
            throw new ResponseStatusException(NOT_FOUND, "1차 카테고리를 찾을 수 없습니다");
        if (subRepo.existsByCategoryIdAndName(req.getCategoryId(), req.getName()))
            throw new ResponseStatusException(CONFLICT, "이미 존재하는 2차 카테고리명입니다");
        return PartySubcategoryDto.Response.from(subRepo.save(PartySubcategory.builder()
            .categoryId(req.getCategoryId()).name(req.getName()).description(req.getDescription())
            .minLevel(req.getMinLevel()).sortOrder(req.getSortOrder()).createdBy(adminId).build()));
    }

    public PartySubcategoryDto.Response updateSubcategory(Long id, PartySubcategoryDto.CreateRequest req) {
        PartySubcategory sub = subRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        sub.setName(req.getName()); sub.setDescription(req.getDescription());
        sub.setMinLevel(req.getMinLevel()); sub.setSortOrder(req.getSortOrder());
        return PartySubcategoryDto.Response.from(subRepo.save(sub));
    }

    public void deleteSubcategory(Long id) {
        PartySubcategory sub = subRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        sub.setActive(false);
        subRepo.save(sub);
    }

    // ── 파티 모집 글 ──────────────────────────────────────────────────────────
    public Page<PartyPostDto.Summary> getPosts(Long catId, Long subId, String server, int page, int size) {
        Page<PartyPost> posts = postRepo.search(catId, subId, server, PageRequest.of(page, size));
        return posts.map(pp -> buildSummary(pp));
    }

    public PartyPostDto.Detail getPost(Long id) {
        PartyPost pp = postRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (pp.getStatus() == PartyPost.Status.DELETED)
            throw new ResponseStatusException(NOT_FOUND);
        postRepo.incrementViews(id);
        Account author = accountRepo.findById(pp.getAccountId()).orElse(null);
        PartyCategory cat = catRepo.findById(pp.getCategoryId()).orElse(null);
        PartySubcategory sub = subRepo.findById(pp.getSubcategoryId()).orElse(null);
        String charName = pp.getCharacterId() != null
            ? charRepo.findById(pp.getCharacterId()).map(Character::getName).orElse(null) : null;
        List<PartyPostDto.CommentResponse> comments = commentRepo
            .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(id)
            .stream().map(c -> {
                Account ca = accountRepo.findById(c.getAccountId()).orElse(null);
                return PartyPostDto.CommentResponse.builder().id(c.getId())
                    .accountId(c.getAccountId())
                    .authorName(ca != null ? ca.getDisplayName() : "탈퇴한 회원")
                    .authorProvider(ca != null ? ca.getAuthProvider().name() : "UNKNOWN")
                    .content(c.getContent()).createdAt(c.getCreatedAt()).build();
            }).toList();
        return PartyPostDto.Detail.builder().id(pp.getId())
            .categoryId(pp.getCategoryId()).categoryName(cat != null ? cat.getName() : "")
            .subcategoryId(pp.getSubcategoryId()).subcategoryName(sub != null ? sub.getName() : "")
            .accountId(pp.getAccountId()).authorName(author != null ? author.getDisplayName() : "탈퇴한 회원")
            .authorProvider(author != null ? author.getAuthProvider().name() : "UNKNOWN")
            .authorProfileImage(author != null ? author.getProfileImage() : null)
            .characterId(pp.getCharacterId()).characterName(charName)
            .title(pp.getTitle()).content(pp.getContent()).server(pp.getServer())
            .minLevel(pp.getMinLevel()).maxMembers(pp.getMaxMembers()).currentMembers(pp.getCurrentMembers())
            .status(pp.getStatus().name()).scheduleTime(pp.getScheduleTime())
            .views(pp.getViews()).createdAt(pp.getCreatedAt()).updatedAt(pp.getUpdatedAt())
            .comments(comments).build();
    }

    public PartyPostDto.Detail createPost(Long aid, PartyPostDto.CreateRequest req) {
        if (!catRepo.existsById(req.getCategoryId()))
            throw new ResponseStatusException(NOT_FOUND, "카테고리를 찾을 수 없습니다");
        if (!subRepo.existsById(req.getSubcategoryId()))
            throw new ResponseStatusException(NOT_FOUND, "세부 카테고리를 찾을 수 없습니다");
        if (req.getCharacterId() != null) {
            charRepo.findByIdAndAccountId(req.getCharacterId(), aid)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "본인 캐릭터만 선택 가능합니다"));
        }
        PartyPost saved = postRepo.save(PartyPost.builder()
            .categoryId(req.getCategoryId()).subcategoryId(req.getSubcategoryId())
            .accountId(aid).characterId(req.getCharacterId())
            .title(req.getTitle()).content(req.getContent()).server(req.getServer())
            .minLevel(req.getMinLevel()).maxMembers(req.getMaxMembers())
            .scheduleTime(req.getScheduleTime()).build());
        return getPost(saved.getId());
    }

    public PartyPostDto.Detail updatePost(Long aid, Long postId, PartyPostDto.CreateRequest req) {
        PartyPost pp = postRepo.findById(postId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!pp.getAccountId().equals(aid)) throw new ResponseStatusException(FORBIDDEN, "수정 권한이 없습니다");
        pp.setTitle(req.getTitle()); pp.setContent(req.getContent());
        pp.setServer(req.getServer()); pp.setMinLevel(req.getMinLevel());
        pp.setMaxMembers(req.getMaxMembers()); pp.setScheduleTime(req.getScheduleTime());
        postRepo.save(pp);
        return getPost(postId);
    }

    public void closePost(Long aid, Long postId) {
        PartyPost pp = postRepo.findById(postId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!pp.getAccountId().equals(aid)) throw new ResponseStatusException(FORBIDDEN);
        pp.setStatus(PartyPost.Status.CLOSED); postRepo.save(pp);
    }

    public void deletePost(Long aid, Long postId, boolean isAdmin) {
        PartyPost pp = postRepo.findById(postId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!isAdmin && !pp.getAccountId().equals(aid)) throw new ResponseStatusException(FORBIDDEN);
        pp.setStatus(PartyPost.Status.DELETED); postRepo.save(pp);
    }

    // ── 댓글 ─────────────────────────────────────────────────────────────────
    public PartyPostDto.CommentResponse addComment(Long aid, Long postId, String content) {
        postRepo.findById(postId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        PartyComment saved = commentRepo.save(PartyComment.builder()
            .postId(postId).accountId(aid).content(content).build());
        Account author = accountRepo.findById(aid).orElse(null);
        return PartyPostDto.CommentResponse.builder().id(saved.getId()).accountId(aid)
            .authorName(author != null ? author.getDisplayName() : "")
            .authorProvider(author != null ? author.getAuthProvider().name() : "LOCAL")
            .content(saved.getContent()).createdAt(saved.getCreatedAt()).build();
    }

    public void deleteComment(Long aid, Long commentId, boolean isAdmin) {
        PartyComment c = commentRepo.findById(commentId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!isAdmin && !c.getAccountId().equals(aid)) throw new ResponseStatusException(FORBIDDEN);
        c.setDeleted(true); commentRepo.save(c);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────
    private PartyPostDto.Summary buildSummary(PartyPost pp) {
        Account author = accountRepo.findById(pp.getAccountId()).orElse(null);
        PartyCategory cat = catRepo.findById(pp.getCategoryId()).orElse(null);
        PartySubcategory sub = subRepo.findById(pp.getSubcategoryId()).orElse(null);
        long cc = commentRepo.countByPostIdAndIsDeletedFalse(pp.getId());
        return PartyPostDto.Summary.builder().id(pp.getId())
            .categoryId(pp.getCategoryId()).categoryName(cat != null ? cat.getName() : "")
            .subcategoryId(pp.getSubcategoryId()).subcategoryName(sub != null ? sub.getName() : "")
            .title(pp.getTitle()).server(pp.getServer()).minLevel(pp.getMinLevel())
            .maxMembers(pp.getMaxMembers()).currentMembers(pp.getCurrentMembers())
            .status(pp.getStatus().name()).views(pp.getViews()).commentCount(cc)
            .authorName(author != null ? author.getDisplayName() : "탈퇴한 회원")
            .authorProvider(author != null ? author.getAuthProvider().name() : "UNKNOWN")
            .createdAt(pp.getCreatedAt()).build();
    }
}
