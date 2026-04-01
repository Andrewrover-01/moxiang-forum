package com.moxiang.service.novel;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Novel;
import com.moxiang.mbg.entity.NovelChapter;

import java.util.List;

/**
 * Novel service interface.
 */
public interface NovelService {

    Novel createNovel(Long userId, String title, String description, String cover, String category);

    Novel getById(Long id);

    IPage<Novel> pageNovels(Page<Novel> page, String category);

    IPage<Novel> pageByUser(Page<Novel> page, Long userId);

    IPage<Novel> search(Page<Novel> page, String keyword);

    void updateNovel(Long novelId, Long userId, String title, String description, String cover, String category, Integer status);

    void deleteNovel(Long novelId, Long userId);

    void incrementViewCount(Long novelId);

    boolean toggleCollect(Long novelId, Long userId);

    boolean isCollected(Long novelId, Long userId);

    // ---- Chapters ----

    NovelChapter addChapter(Long novelId, Long userId, String title, String content);

    NovelChapter getChapter(Long chapterId);

    List<NovelChapter> listChapters(Long novelId);

    void updateChapter(Long chapterId, Long userId, String title, String content);

    void deleteChapter(Long chapterId, Long userId);
}
