package com.aion2.controller;

import com.aion2.dto.*;
import com.aion2.entity.*;
import com.aion2.repository.*;
import com.aion2.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ── AuthController ────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthDto.LoginResponse login(@RequestBody AuthDto.LoginRequest req) {
        return authService.login(req);
    }
}

// ── CharacterController ───────────────────────────────────────────────────────
@RestController
@RequestMapping("/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public List<CharacterDto.Detail> list(@AuthenticationPrincipal Account account) {
        return characterService.findByAccountId(account.getId());
    }

    @PostMapping
    public CharacterDto.Detail add(
            @AuthenticationPrincipal Account account,
            @RequestBody CharacterDto.AddRequest req) {
        return characterService.addCharacter(account.getId(), req);
    }

    @PutMapping("/{id}/refresh")
    public CharacterDto.Detail refresh(
            @AuthenticationPrincipal Account account,
            @PathVariable Long id) {
        return characterService.refreshCharacter(account.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Account account,
            @PathVariable Long id) {
        characterService.deleteCharacter(account.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

// ── QuestCheckController ──────────────────────────────────────────────────────
@RestController
@RequestMapping("/quest-checks")
@RequiredArgsConstructor
public class QuestCheckController {

    private final QuestCheckService questCheckService;

    @GetMapping("/dashboard")
    public List<QuestDto.CharacterDashboard> dashboard(
            @AuthenticationPrincipal Account account) {
        return questCheckService.getDashboard(account.getId());
    }

    @PostMapping("/{charId}/toggle/{questId}")
    public ResponseEntity<Void> toggle(
            @AuthenticationPrincipal Account account,
            @PathVariable Long charId,
            @PathVariable Long questId) {
        questCheckService.toggle(account.getId(), charId, questId);
        return ResponseEntity.ok().build();
    }
}

// ── VerificationController ────────────────────────────────────────────────────
@RestController
@RequestMapping("/characters/{charId}/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/issue")
    public VerificationDto.IssueResponse issueCode(
            @PathVariable Long charId,
            @AuthenticationPrincipal Account account) {
        return verificationService.issueCode(account.getId(), charId);
    }

    @PostMapping("/verify")
    public VerificationDto.VerifyResponse verify(
            @PathVariable Long charId,
            @AuthenticationPrincipal Account account) {
        return verificationService.verify(account.getId(), charId);
    }
}

// ── AdminController ───────────────────────────────────────────────────────────
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/accounts")
    public List<AccountDto.Summary> getAccounts() {
        return adminService.findAllAccounts();
    }

    @PostMapping("/accounts")
    public AccountDto.Summary createAccount(@RequestBody AccountDto.CreateRequest req) {
        return adminService.createAccount(req);
    }

    @GetMapping("/quests")
    public List<QuestDto.Summary> getQuests() {
        return adminService.findAllQuests();
    }

    @PostMapping("/quests")
    public QuestDto.Summary createQuest(
            @RequestBody QuestDto.CreateRequest req,
            @AuthenticationPrincipal Account admin) {
        return adminService.createQuest(req, admin.getId());
    }

    @GetMapping("/config/verify")
    public Map<String, String> getVerifyConfig() {
        return adminService.getVerifyConfig();
    }

    @PutMapping("/config/verify")
    public ResponseEntity<Void> updateVerifyConfig(@RequestBody Map<String, String> config) {
        adminService.updateVerifyConfig(config);
        return ResponseEntity.ok().build();
    }
}

// ── PublicController (비로그인 공개 API) ──────────────────────────────────────
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final CharacterRepository charRepo;
    private final PostRepository postRepo;

    @GetMapping("/ranking")
    public List<RankingDto> getRanking(
            @RequestParam(defaultValue = "카이나토스") String server) {
        var list = charRepo.findTop50ByServerOrderByCombatPowerDesc(server);
        var result = new java.util.ArrayList<RankingDto>();
        for (int i = 0; i < list.size(); i++) {
            var ch = list.get(i);
            result.add(RankingDto.builder()
                .rank(i + 1)
                .displayName(maskName(ch.getName()))
                .className(ch.getClassName())
                .level(ch.getLevel())
                .combatPower(ch.getCombatPower())
                .grade(ch.getGrade())
                .verified(ch.isVerified())
                .build());
        }
        return result;
    }

    @GetMapping("/posts")
    public Page<PostDto.Summary> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (category != null) {
            return postRepo.findByCategory(Post.Category.valueOf(category), pageable)
                .map(PostDto.Summary::from);
        }
        return postRepo.findAll(pageable).map(PostDto.Summary::from);
    }

    @GetMapping("/stats")
    public StatsDto getStats() {
        return StatsDto.builder()
            .totalUsers(charRepo.countDistinctAccountId())
            .totalCharacters(charRepo.count())
            .build();
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 2) return name;
        return name.substring(0, 2) + "*".repeat(name.length() - 2);
    }
}
