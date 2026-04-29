package com.dynatrace.teamtooling.aiworkshop.repository;

import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @EntityGraph(attributePaths = "tags")
    @Query("SELECT t FROM Todo t")
    List<Todo> findAllWithTags();

    @EntityGraph(attributePaths = "tags")
    @Query("SELECT t FROM Todo t WHERE t.id = :id")
    Optional<Todo> findWithTagsById(Long id);
}
