package nz.clem.blog.service;

import nz.clem.blog.dto.*;
import nz.clem.blog.entity.BlockType;
import nz.clem.blog.entity.ContentBlock;
import nz.clem.blog.entity.Page;
import nz.clem.blog.entity.PostStatus;
import nz.clem.blog.repository.ContentBlockRepository;
import nz.clem.blog.repository.PageRepository;
import nz.clem.blog.service.ImageReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PageService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private ContentBlockRepository contentBlockRepository;

    @Autowired
    private ImageReferenceService imageReferenceService;

    // ── Public queries ───────────────────────────────────────────────────────

    public List<PageSummaryDTO> getPublishedPages() {
        return pageRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public Optional<PageDTO> getPublishedPageBySlug(String slug) {
        return pageRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(this::toDTO);
    }

    // ── Admin queries ────────────────────────────────────────────────────────

    public List<PageSummaryDTO> getAllPages() {
        return pageRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public Optional<PageDTO> getPageById(Long id) {
        return pageRepository.findById(id).map(this::toDTO);
    }

    // ── Page CRUD ────────────────────────────────────────────────────────────

    @Transactional
    public PageDTO createPage(CreatePageRequest req) {
        Page page = new Page();
        page.setTitle(req.getTitle());
        page.setSlug(req.getSlug());
        page.setMetaDescription(req.getMetaDescription());
        page.setOgImageUrl(req.getOgImageUrl());
        page.setStatus(parseStatus(req.getStatus()));
        Page savedPage = pageRepository.save(page);
        imageReferenceService.syncForPage(savedPage);
        return toDTO(savedPage);
    }

    @Transactional
    public Optional<PageDTO> updatePage(Long id, UpdatePageRequest req) {
        return pageRepository.findById(id).map(page -> {
            if (req.getTitle() != null) page.setTitle(req.getTitle());
            if (req.getSlug() != null) page.setSlug(req.getSlug());
            if (req.getMetaDescription() != null) page.setMetaDescription(req.getMetaDescription());
            if (req.getOgImageUrl() != null) page.setOgImageUrl(req.getOgImageUrl());
            if (req.getStatus() != null) page.setStatus(parseStatus(req.getStatus()));
            Page savedPage = pageRepository.save(page);
            imageReferenceService.syncForPage(savedPage);
            return toDTO(savedPage);
        });
    }

    @Transactional
    public boolean deletePage(Long id) {
        if (!pageRepository.existsById(id)) return false;
        imageReferenceService.deleteForPage(id);
        pageRepository.deleteById(id);
        return true;
    }

    // ── Block CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public Optional<ContentBlockDTO> addBlock(Long pageId, CreateBlockRequest req) {
        return pageRepository.findById(pageId).map(page -> {
            ContentBlock block = new ContentBlock();
            block.setPage(page);
            block.setBlockType(parseBlockType(req.getBlockType()));
            block.setContent(req.getContent() != null ? req.getContent() : new HashMap<>());

            if (req.getSortOrder() != null) {
                block.setSortOrder(req.getSortOrder());
            } else {
                // Append after the last block
                List<ContentBlock> existing = contentBlockRepository.findByPageIdOrderBySortOrderAsc(pageId);
                block.setSortOrder(existing.isEmpty() ? 0 : existing.getLast().getSortOrder() + 10);
            }

            ContentBlock savedBlock = contentBlockRepository.save(block);
            imageReferenceService.syncForBlock(savedBlock);
            return toBlockDTO(savedBlock);
        });
    }

    @Transactional
    public Optional<ContentBlockDTO> updateBlock(Long pageId, Long blockId, UpdateBlockRequest req) {
        return contentBlockRepository.findById(blockId)
                .filter(b -> b.getPage().getId().equals(pageId))
                .map(block -> {
                    if (req.getBlockType() != null) block.setBlockType(parseBlockType(req.getBlockType()));
                    if (req.getContent() != null) block.setContent(req.getContent());
                    ContentBlock updatedBlock = contentBlockRepository.save(block);
                    imageReferenceService.syncForBlock(updatedBlock);
                    return toBlockDTO(updatedBlock);
                });
    }

    @Transactional
    public boolean deleteBlock(Long pageId, Long blockId) {
        return contentBlockRepository.findById(blockId)
                .filter(b -> b.getPage().getId().equals(pageId))
                .map(block -> {
                    imageReferenceService.deleteForBlock(block.getId());
                    contentBlockRepository.delete(block);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean reorderBlocks(Long pageId, ReorderBlocksRequest req) {
        if (!pageRepository.existsById(pageId)) return false;

        Map<Long, ContentBlock> blockMap = contentBlockRepository
                .findByPageIdOrderBySortOrderAsc(pageId)
                .stream().collect(Collectors.toMap(ContentBlock::getId, b -> b));

        for (ReorderBlocksRequest.BlockOrder order : req.getBlocks()) {
            ContentBlock block = blockMap.get(order.getId());
            if (block == null) return false; // block doesn't belong to this page
            block.setSortOrder(order.getSortOrder());
            contentBlockRepository.save(block);
        }
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PostStatus parseStatus(String status) {
        if (status == null) return PostStatus.DRAFT;
        try {
            return PostStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return PostStatus.DRAFT;
        }
    }

    private BlockType parseBlockType(String blockType) {
        try {
            return BlockType.valueOf(blockType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown block type: " + blockType);
        }
    }

    private PageSummaryDTO toSummaryDTO(Page page) {
        PageSummaryDTO dto = new PageSummaryDTO();
        dto.setId(page.getId());
        dto.setSlug(page.getSlug());
        dto.setTitle(page.getTitle());
        dto.setMetaDescription(page.getMetaDescription());
        dto.setStatus(page.getStatus().name());
        dto.setCreatedAt(page.getCreatedAt());
        dto.setUpdatedAt(page.getUpdatedAt());
        return dto;
    }

    private PageDTO toDTO(Page page) {
        PageDTO dto = new PageDTO();
        dto.setId(page.getId());
        dto.setSlug(page.getSlug());
        dto.setTitle(page.getTitle());
        dto.setMetaDescription(page.getMetaDescription());
        dto.setOgImageUrl(page.getOgImageUrl());
        dto.setStatus(page.getStatus().name());
        dto.setBlocks(page.getBlocks().stream().map(this::toBlockDTO).collect(Collectors.toList()));
        dto.setCreatedAt(page.getCreatedAt());
        dto.setUpdatedAt(page.getUpdatedAt());
        return dto;
    }

    private ContentBlockDTO toBlockDTO(ContentBlock block) {
        ContentBlockDTO dto = new ContentBlockDTO();
        dto.setId(block.getId());
        dto.setBlockType(block.getBlockType().name());
        dto.setSortOrder(block.getSortOrder());
        dto.setContent(block.getContent());
        return dto;
    }
}
