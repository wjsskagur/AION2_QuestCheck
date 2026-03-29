package com.aion2.controller;

import com.aion2.dto.*;
import com.aion2.entity.Account;
import com.aion2.entity.Post;
import com.aion2.repository.*;
import com.aion2.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// ── AuthController ────────────────────────────────────────────────────────────
@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/login")  public AuthDto.LoginResponse login(@RequestBody AuthDto.LoginRequest req)   { return authService.login(req); }
    @PostMapping("/signup") public AuthDto.LoginResponse signup(@RequestBody AuthDto.SignupRequest req) { return authService.signup(req); }
    @GetMapping("/me") public AuthDto.LoginResponse me(@AuthenticationPrincipal Account a) { return authService.getMe(a); }
}

// ── CharacterController ───────────────────────────────────────────────────────
@RestController @RequestMapping("/characters") @RequiredArgsConstructor
public class CharacterController {
    private final CharacterService svc;
    @GetMapping             public List<CharacterDto.Detail> list(@AuthenticationPrincipal Account a)                      { return svc.findByAccountId(a.getId()); }
    @PostMapping            public CharacterDto.Detail add(@AuthenticationPrincipal Account a, @RequestBody CharacterDto.AddRequest req) { return svc.add(a.getId(), req); }
    @PutMapping("/{id}/refresh") public CharacterDto.Detail refresh(@AuthenticationPrincipal Account a, @PathVariable Long id)          { return svc.refresh(a.getId(), id); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal Account a, @PathVariable Long id) { svc.delete(a.getId(), id); return ResponseEntity.noContent().build(); }
}

// ── QuestCheckController ──────────────────────────────────────────────────────
@RestController @RequestMapping("/quest-checks") @RequiredArgsConstructor
public class QuestCheckController {
    private final QuestCheckService svc;
    @GetMapping("/dashboard")                    public List<QuestDto.CharacterDashboard> dashboard(@AuthenticationPrincipal Account a)                          { return svc.getDashboard(a.getId()); }
    @PostMapping("/{cid}/toggle/{qid}")          public ResponseEntity<Void> toggle(@AuthenticationPrincipal Account a, @PathVariable Long cid, @PathVariable Long qid) { svc.toggle(a.getId(), cid, qid); return ResponseEntity.ok().build(); }
}

// ── VerificationController ────────────────────────────────────────────────────
@RestController @RequestMapping("/characters/{cid}/verification") @RequiredArgsConstructor
public class VerificationController {
    private final VerificationService svc;
    @PostMapping("/issue")  public VerificationDto.IssueResponse issue(@PathVariable Long cid, @AuthenticationPrincipal Account a)  { return svc.issue(a.getId(), cid); }
    @PostMapping("/verify") public VerificationDto.VerifyResponse verify(@PathVariable Long cid, @AuthenticationPrincipal Account a) { return svc.verify(a.getId(), cid); }
}

// ── AdminController ───────────────────────────────────────────────────────────
@RestController @RequestMapping("/admin") @RequiredArgsConstructor
public class AdminController {
    private final AdminService svc;
    @GetMapping("/accounts")        public List<AccountDto.Summary> getAccounts()                                         { return svc.findAllAccounts(); }
    @PostMapping("/accounts")       public AccountDto.Summary createAccount(@RequestBody AccountDto.CreateRequest req)    { return svc.createAccount(req); }
    @GetMapping("/quests")          public List<QuestDto.Summary> getQuests()                                             { return svc.findAllQuests(); }
    @PostMapping("/quests")         public QuestDto.Summary createQuest(@RequestBody QuestDto.CreateRequest req, @AuthenticationPrincipal Account a) { return svc.createQuest(req, a.getId()); }
    @GetMapping("/config/verify")   public Map<String, String> getVerifyConfig()                                          { return svc.getVerifyConfig(); }
    @PutMapping("/config/verify")   public ResponseEntity<Void> updateVerifyConfig(@RequestBody Map<String, String> cfg) { svc.updateVerifyConfig(cfg); return ResponseEntity.ok().build(); }
}

// ── PublicController ──────────────────────────────────────────────────────────
@RestController @RequestMapping("/public") @RequiredArgsConstructor
public class PublicController {
    private final CharacterRepository charRepo;
    private final PostRepository postRepo;

    @GetMapping("/ranking")
    public List<RankingDto> ranking(@RequestParam(defaultValue = "카이나토스") String server) {
        var list = charRepo.findTop50ByServerOrderByCombatPowerDesc(server);
        var r = new ArrayList<RankingDto>();
        for (int i = 0; i < list.size(); i++) {
            var c = list.get(i);
            r.add(RankingDto.builder().rank(i+1).displayName(mask(c.getName()))
                .className(c.getClassName()).level(c.getLevel())
                .combatPower(c.getCombatPower()).grade(c.getGrade()).verified(c.isVerified()).build());
        }
        return r;
    }

    @GetMapping("/posts")
    public Page<PostDto.Summary> posts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return category != null
            ? postRepo.findByCategory(Post.Category.valueOf(category), p).map(PostDto.Summary::from)
            : postRepo.findAll(p).map(PostDto.Summary::from);
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return StatsDto.builder().totalUsers(charRepo.countDistinctAccountId()).totalCharacters(charRepo.count()).build();
    }

    private String mask(String n) {
        if (n == null || n.length() <= 2) return n;
        return n.substring(0, 2) + "*".repeat(n.length() - 2);
    }
}

// ── PartyBoardController ──────────────────────────────────────────────────────
@RestController @RequestMapping("/party") @RequiredArgsConstructor
public class PartyBoardController {
    private final PartyBoardService svc;

    // ── 카테고리 (공개) ───────────────────────────────────────────────────────
    @GetMapping("/categories")
    public List<PartyCategoryDto.WithSubs> getCategories() { return svc.getAllCategoriesWithSubs(); }

    // ── 글 목록 / 상세 (공개) ─────────────────────────────────────────────────
    @GetMapping("/posts")
    public Page<PartyPostDto.Summary> getPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String server,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return svc.getPosts(categoryId, subcategoryId, server, page, size);
    }

    @GetMapping("/posts/{id}")
    public PartyPostDto.Detail getPost(@PathVariable Long id) { return svc.getPost(id); }

    // ── 글 작성 / 수정 / 삭제 (로그인) ──────────────────────────────────────
    @PostMapping("/posts")
    public PartyPostDto.Detail createPost(@AuthenticationPrincipal Account a,
            @RequestBody PartyPostDto.CreateRequest req) { return svc.createPost(a.getId(), req); }

    @PutMapping("/posts/{id}")
    public PartyPostDto.Detail updatePost(@AuthenticationPrincipal Account a, @PathVariable Long id,
            @RequestBody PartyPostDto.CreateRequest req) { return svc.updatePost(a.getId(), id, req); }

    @PatchMapping("/posts/{id}/close")
    public ResponseEntity<Void> closePost(@AuthenticationPrincipal Account a, @PathVariable Long id) {
        svc.closePost(a.getId(), id); return ResponseEntity.ok().build();
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@AuthenticationPrincipal Account a, @PathVariable Long id) {
        svc.deletePost(a.getId(), id, a.getRole() == Account.Role.ADMIN); return ResponseEntity.noContent().build();
    }

    // ── 댓글 ─────────────────────────────────────────────────────────────────
    @PostMapping("/posts/{id}/comments")
    public PartyPostDto.CommentResponse addComment(@AuthenticationPrincipal Account a, @PathVariable Long id,
            @RequestBody Map<String, String> body) { return svc.addComment(a.getId(), id, body.get("content")); }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal Account a, @PathVariable Long id) {
        svc.deleteComment(a.getId(), id, a.getRole() == Account.Role.ADMIN); return ResponseEntity.noContent().build();
    }
}

// ── PartyAdminController (관리자 카테고리 관리) ─────────────────────────────
@RestController @RequestMapping("/admin/party") @RequiredArgsConstructor
public class PartyAdminController {
    private final PartyBoardService svc;

    @PostMapping("/categories")
    public PartyCategoryDto.Response createCat(@RequestBody PartyCategoryDto.CreateRequest req, @AuthenticationPrincipal Account a) { return svc.createCategory(req, a.getId()); }
    @PutMapping("/categories/{id}")
    public PartyCategoryDto.Response updateCat(@PathVariable Long id, @RequestBody PartyCategoryDto.CreateRequest req) { return svc.updateCategory(id, req); }
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCat(@PathVariable Long id) { svc.deleteCategory(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/subcategories")
    public PartySubcategoryDto.Response createSub(@RequestBody PartySubcategoryDto.CreateRequest req, @AuthenticationPrincipal Account a) { return svc.createSubcategory(req, a.getId()); }
    @PutMapping("/subcategories/{id}")
    public PartySubcategoryDto.Response updateSub(@PathVariable Long id, @RequestBody PartySubcategoryDto.CreateRequest req) { return svc.updateSubcategory(id, req); }
    @DeleteMapping("/subcategories/{id}")
    public ResponseEntity<Void> deleteSub(@PathVariable Long id) { svc.deleteSubcategory(id); return ResponseEntity.noContent().build(); }
}
