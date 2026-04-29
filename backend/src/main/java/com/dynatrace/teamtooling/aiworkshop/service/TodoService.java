package com.dynatrace.teamtooling.aiworkshop.service;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional(readOnly = true)
    public List<Todo> findFiltered(Boolean done, Priority priority, Long tagId, int page, int size) {
        List<Todo> all = todoRepository.findAllWithTags();

        if (done != null) {
            all = all.stream().filter(t -> t.isDone() == done).collect(Collectors.toList());
        }
        if (priority != null) {
            all = all.stream().filter(t -> t.getPriority() == priority).collect(Collectors.toList());
        }
        if (tagId != null) {
            all = all.stream()
                    .filter(t -> t.getTags().stream().anyMatch(tg -> tg.getId().equals(tagId)))
                    .collect(Collectors.toList());
        }

        all.sort((a, b) -> {
            int p = priorityRank(b.getPriority()) - priorityRank(a.getPriority());
            if (p != 0) return p;
            if (a.getDueDate() == null && b.getDueDate() == null) return 0;
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    private int priorityRank(Priority p) {
        if (p == Priority.HIGH) return 3;
        if (p == Priority.MEDIUM) return 2;
        if (p == Priority.LOW) return 1;
        return 0;
    }

    @Transactional(readOnly = true)
    public Optional<Todo> findById(Long id) {
        return todoRepository.findWithTagsById(id);
    }

    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    public void deleteById(Long id) {
        todoRepository.deleteById(id);
    }
}
