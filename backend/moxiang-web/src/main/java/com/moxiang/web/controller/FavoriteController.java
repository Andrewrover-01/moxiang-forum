package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.Post;
import com.moxiang.service.favorite.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Favorite controller — save/remove post bookmarks, list favorites.
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** Toggle favorite on a post (saves or removes the bookmark). */
    @PostMapping("/post/{postId}")
    public CommonResult<Map<String, Boolean>> toggleFavorite(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        boolean favorited = favoriteService.toggleFavorite(postId, userId);
        return CommonResult.success(Map.of("favorited", favorited));
    }

    /** Check whether the current user has favorited a post. */
    @GetMapping("/post/{postId}/status")
    public CommonResult<Map<String, Boolean>> favoriteStatus(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        return CommonResult.success(Map.of("favorited", favoriteService.isFavorited(postId, userId)));
    }

    /** List posts favorited by the current user (paginated). */
    @GetMapping("/posts")
    public CommonResult<?> listFavoritePosts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = getCurrentUserId();
        return CommonResult.success(favoriteService.pageFavoritePosts(new Page<>(current, size), userId));
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
