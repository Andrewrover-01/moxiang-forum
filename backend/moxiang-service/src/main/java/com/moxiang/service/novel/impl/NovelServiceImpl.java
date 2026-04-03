package com.moxiang.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.constant.ContentType;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Novel;
import com.moxiang.mbg.entity.NovelChapter;
import com.moxiang.mbg.mapper.NovelChapterMapper;
import com.moxiang.mbg.mapper.NovelMapper;
import com.moxiang.service.moderation.ContentModerationService;
import com.moxiang.service.novel.NovelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Novel service implementation.
 */
@Service
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel> implements NovelService {

    private final NovelChapterMapper chapterMapper;
    private final RedisUtils redisUtils;
    private final ContentModerationService moderationService;

    public NovelServiceImpl(NovelChapterMapper chapterMapper, RedisUtils redisUtils,
                            ContentModerationService moderationService) {
        this.chapterMapper = chapterMapper;
        this.redisUtils = redisUtils;
        this.moderationService = moderationService;
    }

    @Override
    public Novel createNovel(Long userId, String title, String description, String cover, String category) {
        Novel novel = new Novel()
                .setUserId(userId)
                .setTitle(title)
                .setDescription(description)
                .setCover(cover)
                .setCategory(category)
                .setWordCount(0L)
                .setChapterCount(0)
                .setViewCount(0L)
                .setCollectCount(0L)
                .setStatus(0);
        save(novel);
        return novel;
    }

    @Override
    public Novel getById(Long id) {
        Novel novel = super.getById(id);
        if (novel == null) {
            throw new BusinessException(ResultCode.NOVEL_NOT_FOUND);
        }
        return novel;
    }

    @Override
    public IPage<Novel> pageNovels(Page<Novel> page, String category) {
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<Novel>()
                .eq(StringUtils.hasText(category), Novel::getCategory, category)
                .orderByDesc(Novel::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public IPage<Novel> pageByUser(Page<Novel> page, Long userId) {
        return page(page, new LambdaQueryWrapper<Novel>()
                .eq(Novel::getUserId, userId)
                .orderByDesc(Novel::getCreatedAt));
    }

    @Override
    public IPage<Novel> search(Page<Novel> page, String keyword) {
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<Novel>()
                .and(StringUtils.hasText(keyword),
                        w -> w.like(Novel::getTitle, keyword).or().like(Novel::getDescription, keyword))
                .orderByDesc(Novel::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void updateNovel(Long novelId, Long userId, String title, String description,
                            String cover, String category, Integer status) {
        Novel novel = requireNovel(novelId);
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (title != null) novel.setTitle(title);
        if (description != null) novel.setDescription(description);
        if (cover != null) novel.setCover(cover);
        if (category != null) novel.setCategory(category);
        if (status != null) novel.setStatus(status);
        updateById(novel);
    }

    @Override
    @Transactional
    public void deleteNovel(Long novelId, Long userId) {
        Novel novel = requireNovel(novelId);
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        removeById(novelId);
        // Soft-delete all chapters
        chapterMapper.delete(new LambdaQueryWrapper<NovelChapter>().eq(NovelChapter::getNovelId, novelId));
    }

    @Override
    public void incrementViewCount(Long novelId) {
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Novel>()
                        .eq(Novel::getId, novelId)
                        .setSql("view_count = view_count + 1"));
    }

    @Override
    public boolean toggleCollect(Long novelId, Long userId) {
        String key = AuthConstants.NOVEL_COLLECT_PREFIX + novelId;
        String member = String.valueOf(userId);
        if (redisUtils.setIsMember(key, member)) {
            redisUtils.setRemove(key, member);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Novel>()
                            .eq(Novel::getId, novelId)
                            .setSql("collect_count = GREATEST(collect_count - 1, 0)"));
            return false;
        } else {
            redisUtils.setAdd(key, member);
            redisUtils.expire(key, 30, TimeUnit.DAYS);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Novel>()
                            .eq(Novel::getId, novelId)
                            .setSql("collect_count = collect_count + 1"));
            return true;
        }
    }

    @Override
    public boolean isCollected(Long novelId, Long userId) {
        String key = AuthConstants.NOVEL_COLLECT_PREFIX + novelId;
        return redisUtils.setIsMember(key, String.valueOf(userId));
    }

    // ---- Chapter operations ----

    @Override
    @Transactional
    public NovelChapter addChapter(Long novelId, Long userId, String title, String content) {
        Novel novel = requireNovel(novelId);
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        long wordCount = content == null ? 0 : content.length();
        int nextChapterNumber = novel.getChapterCount() + 1;

        NovelChapter chapter = new NovelChapter()
                .setNovelId(novelId)
                .setChapterNumber(nextChapterNumber)
                .setTitle(title)
                .setContent(content)
                .setWordCount(wordCount);
        chapterMapper.insert(chapter);

        // Machine review — chapters are high-risk content (novels can contain inappropriate material)
        moderationService.machineReview(ContentType.NOVEL_CHAPTER, chapter.getId(), userId,
                title, content);

        // Update novel stats
        novel.setChapterCount(nextChapterNumber)
             .setWordCount(novel.getWordCount() + wordCount);
        updateById(novel);

        return chapter;
    }

    @Override
    public NovelChapter getChapter(Long chapterId) {
        NovelChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.CHAPTER_NOT_FOUND);
        }
        return chapter;
    }

    @Override
    public List<NovelChapter> listChapters(Long novelId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getNovelId, novelId)
                .orderByAsc(NovelChapter::getChapterNumber));
    }

    @Override
    @Transactional
    public void updateChapter(Long chapterId, Long userId, String title, String content) {
        NovelChapter chapter = getChapter(chapterId);
        Novel novel = requireNovel(chapter.getNovelId());
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        long oldWordCount = chapter.getWordCount() == null ? 0 : chapter.getWordCount();
        long newWordCount = content == null ? 0 : content.length();

        chapter.setTitle(title).setContent(content).setWordCount(newWordCount);
        chapterMapper.updateById(chapter);

        // Recalculate novel word count
        novel.setWordCount(novel.getWordCount() - oldWordCount + newWordCount);
        updateById(novel);
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId, Long userId) {
        NovelChapter chapter = getChapter(chapterId);
        Novel novel = requireNovel(chapter.getNovelId());
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        chapterMapper.deleteById(chapterId);
        novel.setChapterCount(Math.max(0, novel.getChapterCount() - 1))
             .setWordCount(Math.max(0, novel.getWordCount() - (chapter.getWordCount() == null ? 0 : chapter.getWordCount())));
        updateById(novel);
    }

    // ---- Helpers ----

    private Novel requireNovel(Long novelId) {
        Novel novel = super.getById(novelId);
        if (novel == null) {
            throw new BusinessException(ResultCode.NOVEL_NOT_FOUND);
        }
        return novel;
    }
}
