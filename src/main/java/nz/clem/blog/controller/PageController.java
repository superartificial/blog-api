package nz.clem.blog.controller;

import nz.clem.blog.dto.*;
import nz.clem.blog.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pages")
public class PageController {

    @Autowired
    private PageService pageService;

    // ── Public endpoints ─────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<PageSummaryDTO>> getPublishedPages() {
        return ResponseEntity.ok(pageService.getPublishedPages());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PageDTO> getPageBySlug(@PathVariable String slug) {
        return pageService.getPublishedPageBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PageSummaryDTO>> getAllPages() {
        return ResponseEntity.ok(pageService.getAllPages());
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageDTO> getPageById(@PathVariable Long id) {
        return pageService.getPageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPage(@RequestBody CreatePageRequest req) {
        try {
            PageDTO created = pageService.createPage(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A page with slug '" + req.getSlug() + "' already exists"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePage(@PathVariable Long id, @RequestBody UpdatePageRequest req) {
        try {
            return pageService.updatePage(id, req)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A page with slug '" + req.getSlug() + "' already exists"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        return pageService.deletePage(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ── Block endpoints ──────────────────────────────────────────────────────

    @PostMapping("/{pageId}/blocks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addBlock(@PathVariable Long pageId, @RequestBody CreateBlockRequest req) {
        try {
            return pageService.addBlock(pageId, req)
                    .map(b -> ResponseEntity.status(HttpStatus.CREATED).body((Object) b))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{pageId}/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBlock(
            @PathVariable Long pageId,
            @PathVariable Long blockId,
            @RequestBody UpdateBlockRequest req
    ) {
        try {
            return pageService.updateBlock(pageId, blockId, req)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{pageId}/blocks/{blockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long pageId, @PathVariable Long blockId) {
        return pageService.deleteBlock(pageId, blockId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{pageId}/blocks/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reorderBlocks(
            @PathVariable Long pageId,
            @RequestBody ReorderBlocksRequest req
    ) {
        return pageService.reorderBlocks(pageId, req)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
