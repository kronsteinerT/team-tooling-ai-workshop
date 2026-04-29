package com.dynatrace.teamtooling.aiworkshop.controller;

import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class TagController {

    @Autowired
    private TagRepository tagRepository;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<Tag> tags = tagRepository.findAll();
            tags.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            Optional<Tag> tag = tagRepository.findById(id);
            if (tag.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            return ResponseEntity.ok(tag.get());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
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
            Tag saved = tagRepository.save(tag);
            System.out.println("DEBUG: created tag id=" + saved.getId() + " name=" + saved.getName());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Optional<Tag> opt = tagRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            Tag tag = opt.get();
            String name = (String) body.get("name");
            if (name != null && !name.isBlank()) {
                tag.setName(name);
            }
            return ResponseEntity.ok(tagRepository.save(tag));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            tagRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }
}
