package com.dynatrace.teamtooling.aiworkshop.dto;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TodoCreateRequest(
        @NotBlank(message = "title required")
        @Size(max = 200, message = "title too long")
        String title,

        String description,
        boolean done,
        Priority priority,
        LocalDate dueDate,
        List<Long> tagIds
) {}
