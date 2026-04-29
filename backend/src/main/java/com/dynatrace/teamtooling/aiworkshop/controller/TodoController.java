package com.dynatrace.teamtooling.aiworkshop.controller;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import com.dynatrace.teamtooling.aiworkshop.repository.TagRepository;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import com.dynatrace.teamtooling.aiworkshop.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TagRepository tagRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String done,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        try {
            System.out.println("DEBUG: GET /api/todos done=" + done + " priority=" + priority + " tagId=" + tagId + " page=" + page);

            List<Todo> all = todoService.findAll();

            if (done != null && !done.isEmpty()) {
                boolean d = "true".equalsIgnoreCase(done);
                List<Todo> filtered = new ArrayList<>();
                for (Todo t : all) {
                    if (t.isDone() == d) {
                        filtered.add(t);
                    }
                }
                all = filtered;
            }

            if (priority != null && !priority.isEmpty()) {
                List<Todo> filtered = new ArrayList<>();
                for (Todo t : all) {
                    if ("HIGH".equals(priority) && t.getPriority() == Priority.HIGH) {
                        filtered.add(t);
                    } else if ("MEDIUM".equals(priority) && t.getPriority() == Priority.MEDIUM) {
                        filtered.add(t);
                    } else if ("LOW".equals(priority) && t.getPriority() == Priority.LOW) {
                        filtered.add(t);
                    }
                }
                all = filtered;
            }

            if (tagId != null) {
                List<Todo> filtered = new ArrayList<>();
                for (Todo t : all) {
                    for (Tag tg : t.getTags()) {
                        if (tg.getId().equals(tagId)) {
                            filtered.add(t);
                            break;
                        }
                    }
                }
                all = filtered;
            }

            // sort by priority then due date
            all = all.stream().sorted((a, b) -> {
                int p = priorityRank(b.getPriority()) - priorityRank(a.getPriority());
                if (p != 0) return p;
                if (a.getDueDate() == null && b.getDueDate() == null) return 0;
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            }).collect(Collectors.toList());

            int from = Math.min(page * size, all.size());
            int to = Math.min(from + size, all.size());
            List<Todo> paged = all.subList(from, to);

            // simulated slow query — makes the filter race in the UI demonstrable
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            return ResponseEntity.ok(paged);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error");
        }
    }

    private int priorityRank(Priority p) {
        if (p == null) return 0;
        if (p == Priority.HIGH) return 3;
        if (p == Priority.MEDIUM) return 2;
        return 1;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            Optional<Todo> todo = todoService.findById(id);
            if (todo.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            // touch tags to load them lazily before serialization
            todo.get().getTags().size();
            return ResponseEntity.ok(todo.get());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error");
        }
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("title") == null || body.get("title").toString().isBlank()) {
                return ResponseEntity.badRequest().body("title required");
            }
            if (body.get("title").toString().length() > 200) {
                return ResponseEntity.badRequest().body("title too long");
            }

            Todo todo = new Todo();
            todo.setTitle((String) body.get("title"));
            todo.setDescription((String) body.get("description"));

            Object doneObj = body.get("done");
            todo.setDone(doneObj instanceof Boolean ? (Boolean) doneObj : false);

            String prio = (String) body.get("priority");
            if (prio == null) {
                todo.setPriority(Priority.MEDIUM);
            } else if ("HIGH".equals(prio)) {
                todo.setPriority(Priority.HIGH);
            } else if ("LOW".equals(prio)) {
                todo.setPriority(Priority.LOW);
            } else {
                todo.setPriority(Priority.MEDIUM);
            }

            String dueDate = (String) body.get("dueDate");
            if (dueDate != null && !dueDate.isEmpty()) {
                todo.setDueDate(LocalDate.parse(dueDate));
            }

            todo.setCreatedAt(LocalDateTime.now());

            List<Object> tagIds = (List<Object>) body.get("tagIds");
            if (tagIds != null) {
                Set<Tag> tags = new HashSet<>();
                for (Object tid : tagIds) {
                    Long tagIdLong = ((Number) tid).longValue();
                    Optional<Tag> tag = tagRepository.findById(tagIdLong);
                    tag.ifPresent(tags::add);
                }
                todo.setTags(tags);
            }

            Todo saved = todoService.save(todo);
            System.out.println("DEBUG: created todo id=" + saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Optional<Todo> existingOpt = todoService.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            Todo existing = existingOpt.get();

            if (body.containsKey("title")) {
                String t = (String) body.get("title");
                if (t == null || t.isBlank()) {
                    return ResponseEntity.badRequest().body("title required");
                }
                if (t.length() > 200) {
                    return ResponseEntity.badRequest().body("title too long");
                }
                existing.setTitle(t);
            }
            if (body.containsKey("description")) {
                existing.setDescription((String) body.get("description"));
            }
            if (body.containsKey("done")) {
                Object d = body.get("done");
                existing.setDone(d instanceof Boolean ? (Boolean) d : false);
            }
            if (body.containsKey("priority")) {
                String prio = (String) body.get("priority");
                if ("HIGH".equals(prio)) existing.setPriority(Priority.HIGH);
                else if ("LOW".equals(prio)) existing.setPriority(Priority.LOW);
                else existing.setPriority(Priority.MEDIUM);
            }
            if (body.containsKey("dueDate")) {
                String d = (String) body.get("dueDate");
                if (d == null || d.isEmpty()) {
                    existing.setDueDate(null);
                } else {
                    existing.setDueDate(LocalDate.parse(d));
                }
            }
            if (body.containsKey("tagIds")) {
                List<Object> tagIds = (List<Object>) body.get("tagIds");
                Set<Tag> tags = new HashSet<>();
                if (tagIds != null) {
                    for (Object tid : tagIds) {
                        Long tagIdLong = ((Number) tid).longValue();
                        Optional<Tag> tag = tagRepository.findById(tagIdLong);
                        tag.ifPresent(tags::add);
                    }
                }
                existing.setTags(tags);
            }

            // simulate slow update so the toggle race condition is reproducible
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            Todo saved = todoService.save(existing);
            saved.getTags().size();
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            todoService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error");
        }
    }

    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<?> attachTag(@PathVariable Long id, @PathVariable Long tagId) {
        try {
            Optional<Todo> opt = todoService.findById(id);
            Optional<Tag> tagOpt = tagRepository.findById(tagId);
            if (opt.isEmpty() || tagOpt.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            Todo todo = opt.get();
            todo.getTags().add(tagOpt.get());
            todoRepository.save(todo);
            return ResponseEntity.ok(todo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<?> detachTag(@PathVariable Long id, @PathVariable Long tagId) {
        try {
            Optional<Todo> opt = todoService.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body("not found");
            }
            Todo todo = opt.get();
            todo.getTags().removeIf(t -> t.getId().equals(tagId));
            todoRepository.save(todo);
            return ResponseEntity.ok(todo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
    }
}
