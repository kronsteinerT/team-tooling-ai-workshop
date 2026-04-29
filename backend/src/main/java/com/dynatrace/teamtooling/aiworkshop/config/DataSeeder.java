package com.dynatrace.teamtooling.aiworkshop.config;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import com.dynatrace.teamtooling.aiworkshop.repository.TagRepository;
import com.dynatrace.teamtooling.aiworkshop.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Override
    public void run(String... args) {
        if (todoRepository.count() > 0) return;

        System.out.println("DEBUG: seeding database");

        Tag work = saveTag("work");
        Tag personal = saveTag("personal");
        Tag urgent = saveTag("urgent");
        Tag shopping = saveTag("shopping");
        Tag ideas = saveTag("ideas");

        seed("Prepare slides for Q2 review", "Cover migration progress and OKRs", false, Priority.HIGH, LocalDate.now().plusDays(2), Set.of(work, urgent));
        seed("Refactor TodoController", "Split into proper layers", false, Priority.MEDIUM, LocalDate.now().plusDays(7), Set.of(work));
        seed("Buy groceries", "Milk, bread, eggs, coffee", false, Priority.LOW, LocalDate.now().plusDays(1), Set.of(personal, shopping));
        seed("Renew gym membership", "", true, Priority.LOW, LocalDate.now().minusDays(3), Set.of(personal));
        seed("Investigate flaky CI", "test_user_creation fails 1/10 runs", false, Priority.HIGH, LocalDate.now().plusDays(3), Set.of(work, urgent));
        seed("Write blog post", "Spring Boot + JPA gotchas", false, Priority.LOW, LocalDate.now().plusDays(14), Set.of(ideas, personal));
        seed("Review PR #482", "Carlo's auth refactor", false, Priority.MEDIUM, LocalDate.now().plusDays(1), Set.of(work));
        seed("Book dentist", "Annual checkup", false, Priority.MEDIUM, LocalDate.now().plusDays(10), Set.of(personal));
        seed("Update dependencies", "Spring Boot 3.3 -> 3.4", false, Priority.LOW, LocalDate.now().plusDays(30), Set.of(work));
        seed("Plan team offsite", "Venue, agenda, budget", false, Priority.MEDIUM, LocalDate.now().plusDays(21), Set.of(work, ideas));
        seed("Order new keyboard", "ZSA Voyager", true, Priority.LOW, LocalDate.now().minusDays(7), Set.of(personal, shopping));
        seed("Read 'A Philosophy of Software Design'", "", false, Priority.LOW, null, Set.of(personal, ideas));
        seed("Fix N+1 in dashboard", "p99 latency 800ms", false, Priority.HIGH, LocalDate.now().plusDays(4), Set.of(work, urgent));
        seed("Cancel old streaming subscriptions", "", true, Priority.LOW, LocalDate.now().minusDays(1), Set.of(personal));
        seed("Prototype AI assistant idea", "Focus on dev workflow integration", false, Priority.MEDIUM, LocalDate.now().plusDays(60), Set.of(ideas));
    }

    private Tag saveTag(String name) {
        Tag t = new Tag();
        t.setName(name);
        return tagRepository.save(t);
    }

    private void seed(String title, String desc, boolean done, Priority prio, LocalDate due, Set<Tag> tags) {
        Todo t = new Todo();
        t.setTitle(title);
        t.setDescription(desc);
        t.setDone(done);
        t.setPriority(prio);
        t.setDueDate(due);
        t.setCreatedAt(LocalDateTime.now());
        t.setTags(new HashSet<>(tags));
        todoRepository.save(t);
    }
}
