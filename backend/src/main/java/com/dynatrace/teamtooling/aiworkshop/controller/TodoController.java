package com.dynatrace.teamtooling.aiworkshop.controller;

import com.dynatrace.teamtooling.aiworkshop.dto.TodoCreateRequest;
import com.dynatrace.teamtooling.aiworkshop.dto.TodoResponse;
import com.dynatrace.teamtooling.aiworkshop.dto.TodoUpdateRequest;
import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import com.dynatrace.teamtooling.aiworkshop.repository.TagRepository;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import com.dynatrace.teamtooling.aiworkshop.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    private final TodoService todoService;
    private final TodoRepository todoRepository;
    private final TagRepository tagRepository;

    public TodoController(TodoService todoService, TodoRepository todoRepository, TagRepository tagRepository) {
        this.todoService = todoService;
        this.todoRepository = todoRepository;
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String done,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Boolean doneFilter = done != null && !done.isEmpty() ? "true".equalsIgnoreCase(done) : null;
        Priority priorityFilter = priority != null && !priority.isEmpty() ? Priority.valueOf(priority) : null;

        // simulated slow query — makes the filter race in the UI demonstrable
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}

        List<TodoResponse> result = todoService.findFiltered(doneFilter, priorityFilter, tagId, page, size)
                .stream().map(TodoResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Optional<Todo> todo = todoService.findById(id);
        if (todo.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        return ResponseEntity.ok(TodoResponse.from(todo.get()));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TodoCreateRequest body) {
        Todo todo = new Todo();
        todo.setTitle(body.title());
        todo.setDescription(body.description());
        todo.setDone(body.done());
        todo.setPriority(body.priority() != null ? body.priority() : Priority.MEDIUM);
        todo.setDueDate(body.dueDate());
        todo.setCreatedAt(LocalDateTime.now());

        if (body.tagIds() != null) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : body.tagIds()) {
                tagRepository.findById(tagId).ifPresent(tags::add);
            }
            todo.setTags(tags);
        }

        return ResponseEntity.ok(TodoResponse.from(todoService.save(todo)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TodoUpdateRequest body) {
        Optional<Todo> existingOpt = todoService.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        Todo existing = existingOpt.get();

        if (body.title() != null) existing.setTitle(body.title());
        if (body.description() != null) existing.setDescription(body.description());
        if (body.done() != null) existing.setDone(body.done());
        if (body.priority() != null) existing.setPriority(body.priority());
        if (body.dueDate() != null) existing.setDueDate(body.dueDate());
        if (body.tagIds() != null) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : body.tagIds()) {
                tagRepository.findById(tagId).ifPresent(tags::add);
            }
            existing.setTags(tags);
        }

        // simulate slow update so the toggle race condition is reproducible
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        return ResponseEntity.ok(TodoResponse.from(todoService.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        todoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<?> attachTag(@PathVariable Long id, @PathVariable Long tagId) {
        Optional<Todo> opt = todoService.findById(id);
        Optional<Tag> tagOpt = tagRepository.findById(tagId);
        if (opt.isEmpty() || tagOpt.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        Todo todo = opt.get();
        todo.getTags().add(tagOpt.get());
        return ResponseEntity.ok(TodoResponse.from(todoRepository.save(todo)));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<?> detachTag(@PathVariable Long id, @PathVariable Long tagId) {
        Optional<Todo> opt = todoService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("not found");
        }
        Todo todo = opt.get();
        todo.getTags().removeIf(t -> t.getId().equals(tagId));
        return ResponseEntity.ok(TodoResponse.from(todoRepository.save(todo)));
    }
}
