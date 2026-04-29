package com.dynatrace.teamtooling.aiworkshop.dto;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import com.dynatrace.teamtooling.aiworkshop.model.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean done,
        Priority priority,
        LocalDate dueDate,
        LocalDateTime createdAt,
        Set<TagResponse> tags
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isDone(),
                todo.getPriority(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getTags().stream().map(TagResponse::from).collect(Collectors.toSet())
        );
    }
}
