package com.dynatrace.teamtooling.aiworkshop.dto;

import com.dynatrace.teamtooling.aiworkshop.model.Priority;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TodoUpdateRequest(
        @Size(min = 1, max = 200, message = "title must be between 1 and 200 characters")
        String title,

        String description,
        Boolean done,
        Priority priority,
        LocalDate dueDate,
        List<Long> tagIds
) {}
