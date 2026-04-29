package com.dynatrace.teamtooling.aiworkshop;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TodoFlakyTest {

    @Autowired
    private TodoRepository todoRepository;

    @Test
    void createdAtIsWithinRecentWindow() throws Exception {
        LocalDateTime before = LocalDateTime.now();

        Todo todo = new Todo();
        todo.setTitle("flaky-check");
        todo.setPriority(Priority.LOW);
        todo.setCreatedAt(LocalDateTime.now());
        Todo saved = todoRepository.save(todo);

        Thread.sleep(50);

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getCreatedAt().isAfter(before));
        assertTrue(saved.getCreatedAt().isBefore(after));
    }
}
