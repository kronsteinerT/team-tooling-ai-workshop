package com.dynatrace.teamtooling.aiworkshop.repository;

import com.dynatrace.teamtooling.aiworkshop.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
