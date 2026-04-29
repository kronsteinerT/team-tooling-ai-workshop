package com.dynatrace.teamtooling.aiworkshop.controller;

import com.dynatrace.teamtooling.aiworkshop.dto.TagResponse;
import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.repository.TagRepository;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class TagController {

    private final TagRepository tagRepository;
    private final TodoRepository todoRepository;

    public TagController(TagRepository tagRepository, TodoRepository todoRepository) {
        this.tagRepository = tagRepository;
        this.todoRepository = todoRepository;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        List<TagResponse> tags = tagRepository.findAll().stream()
                .map(TagResponse::from)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Optional<Tag> tag = tagRepository.findById(id);
        if (tag.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        return ResponseEntity.ok(TagResponse.from(tag.get()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("name required");
        }
        if (name.length() > 50) {
            return ResponseEntity.badRequest().body("name too long");
        }
        if (tagRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body("tag already exists");
        }
        Tag tag = new Tag();
        tag.setName(name);
        return ResponseEntity.ok(TagResponse.from(tagRepository.save(tag)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<Tag> opt = tagRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        Tag tag = opt.get();
        String name = (String) body.get("name");
        if (name != null && !name.isBlank()) {
            tag.setName(name);
        }
        return ResponseEntity.ok(TagResponse.from(tagRepository.save(tag)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (tagRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        todoRepository.findAllWithTags().forEach(todo -> {
            if (todo.getTags().removeIf(t -> t.getId().equals(id))) {
                todoRepository.save(todo);
            }
        });
        tagRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
