package nz.clem.blog.controller;

import nz.clem.blog.dto.CategoryDTO;
import nz.clem.blog.entity.Category;
import nz.clem.blog.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAll() {
        List<Category> roots = categoryRepository.findByParentIdIsNullOrderBySortOrderAscNameAsc();
        List<CategoryDTO> result = roots.stream().map(root -> {
            List<CategoryDTO> children = categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(root.getId())
                    .stream().map(child -> new CategoryDTO(child.getId(), child.getName(), child.getSlug(), child.getParentId(), List.of(), child.getSortOrder()))
                    .collect(Collectors.toList());
            return new CategoryDTO(root.getId(), root.getName(), root.getSlug(), null, children, root.getSortOrder());
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CategoryDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Name is required");
        }
        if (dto.getSlug() == null || dto.getSlug().isBlank()) {
            return ResponseEntity.badRequest().body("Slug is required");
        }
        if (categoryRepository.findBySlug(dto.getSlug()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Slug already exists");
        }
        if (dto.getParentId() != null && categoryRepository.findById(dto.getParentId()).isEmpty()) {
            return ResponseEntity.badRequest().body("Parent category not found");
        }
        // Place new category at the end of its sibling group
        int maxOrder = categoryRepository.findByParentIdIsNullOrderBySortOrderAscNameAsc()
                .stream().mapToInt(Category::getSortOrder).max().orElse(-1);
        if (dto.getParentId() != null) {
            maxOrder = categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(dto.getParentId())
                    .stream().mapToInt(Category::getSortOrder).max().orElse(-1);
        }
        Category toSave = new Category(null, dto.getName(), dto.getSlug(), dto.getParentId(), maxOrder + 1);
        Category saved = categoryRepository.save(toSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        Optional<Category> existing = categoryRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Optional<Category> slugConflict = categoryRepository.findBySlug(dto.getSlug());
        if (slugConflict.isPresent() && !slugConflict.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Slug already exists");
        }
        if (id.equals(dto.getParentId())) {
            return ResponseEntity.badRequest().body("A category cannot be its own parent");
        }

        Category cat = existing.get();
        cat.setName(dto.getName());
        cat.setSlug(dto.getSlug());
        cat.setParentId(dto.getParentId());
        return ResponseEntity.ok(toDTO(categoryRepository.save(cat)));
    }

    record ReorderItem(Long id, int sortOrder) {}

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorder(@RequestBody List<ReorderItem> items) {
        for (ReorderItem item : items) {
            categoryRepository.findById(item.id()).ifPresent(cat -> {
                cat.setSortOrder(item.sortOrder());
                categoryRepository.save(cat);
            });
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (categoryRepository.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CategoryDTO toDTO(Category c) {
        return new CategoryDTO(c.getId(), c.getName(), c.getSlug(), c.getParentId(), List.of(), c.getSortOrder());
    }
}
