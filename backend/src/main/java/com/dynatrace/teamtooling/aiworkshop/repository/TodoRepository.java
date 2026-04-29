package com.dynatrace.teamtooling.aiworkshop.repository;

import com.dynatrace.teamtooling.aiworkshop.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}
