package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.Novel;
import com.moxiang.mbg.entity.NovelChapter;
import com.moxiang.service.novel.NovelService;
import com.moxiang.web.dto.NovelCreateDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Novel controller — CRUD, chapters, search, collect.
 */
@RestController
@RequestMapping("/api/novel")
public class NovelController {

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    // ---- Novel CRUD ----

    @PostMapping
    public CommonResult<Novel> createNovel(@Valid @RequestBody NovelCreateDTO dto) {
        Long userId = getCurrentUserId();
        Novel novel = novelService.createNovel(userId, dto.getTitle(),
                dto.getDescription(), dto.getCover(), dto.getCategory());
        return CommonResult.success(novel);
    }

    @GetMapping("/{id}")
    public CommonResult<Novel> getNovel(@PathVariable Long id) {
        Novel novel = novelService.getById(id);
        novelService.incrementViewCount(id);
        return CommonResult.success(novel);
    }

    @GetMapping("/list")
    public CommonResult<?> listNovels(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(novelService.pageNovels(new Page<>(current, size), category));
    }

    @GetMapping("/search")
    public CommonResult<?> searchNovels(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(novelService.search(new Page<>(current, size), keyword));
    }

    @GetMapping("/user/{userId}")
    public CommonResult<?> listUserNovels(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(novelService.pageByUser(new Page<>(current, size), userId));
    }

    @PutMapping("/{id}")
    public CommonResult<Void> updateNovel(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        novelService.updateNovel(id, userId,
                (String) body.get("title"),
                (String) body.get("description"),
                (String) body.get("cover"),
                (String) body.get("category"),
                body.get("status") != null ? (Integer) body.get("status") : null);
        return CommonResult.success();
    }

    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteNovel(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        novelService.deleteNovel(id, userId);
        return CommonResult.success();
    }

    @PostMapping("/{id}/collect")
    public CommonResult<Map<String, Boolean>> toggleCollect(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean collected = novelService.toggleCollect(id, userId);
        return CommonResult.success(Map.of("collected", collected));
    }

    @GetMapping("/{id}/collect/status")
    public CommonResult<Map<String, Boolean>> collectStatus(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return CommonResult.success(Map.of("collected", novelService.isCollected(id, userId)));
    }

    // ---- Chapter CRUD ----

    @PostMapping("/{novelId}/chapter")
    public CommonResult<NovelChapter> addChapter(
            @PathVariable Long novelId,
            @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        NovelChapter chapter = novelService.addChapter(novelId, userId,
                body.get("title"), body.get("content"));
        return CommonResult.success(chapter);
    }

    @GetMapping("/{novelId}/chapters")
    public CommonResult<List<NovelChapter>> listChapters(@PathVariable Long novelId) {
        return CommonResult.success(novelService.listChapters(novelId));
    }

    @GetMapping("/chapter/{chapterId}")
    public CommonResult<NovelChapter> getChapter(@PathVariable Long chapterId) {
        return CommonResult.success(novelService.getChapter(chapterId));
    }

    @PutMapping("/chapter/{chapterId}")
    public CommonResult<Void> updateChapter(@PathVariable Long chapterId,
                                             @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        novelService.updateChapter(chapterId, userId, body.get("title"), body.get("content"));
        return CommonResult.success();
    }

    @DeleteMapping("/chapter/{chapterId}")
    public CommonResult<Void> deleteChapter(@PathVariable Long chapterId) {
        Long userId = getCurrentUserId();
        novelService.deleteChapter(chapterId, userId);
        return CommonResult.success();
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (Long) auth.getCredentials();
    }
}
