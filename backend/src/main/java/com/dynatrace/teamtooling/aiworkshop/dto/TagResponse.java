package com.dynatrace.teamtooling.aiworkshop.dto;

import com.dynatrace.teamtooling.aiworkshop.model.Tag;

public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
