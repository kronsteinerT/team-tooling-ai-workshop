package com.dynatrace.teamtooling.aiworkshop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTodosReturns200WithArray() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].done").exists());
    }

    @Test
    void createTodoReturns400WhenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTodoReturns400WhenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAndGetTodo() throws Exception {
        String response = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test Todo\", \"priority\": \"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Todo"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.done").value(false))
                .andReturn().getResponse().getContentAsString();

        String id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asText();

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id)));
    }

    @Test
    void getNonExistentTodoReturns404() throws Exception {
        mockMvc.perform(get("/api/todos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByDoneReturnsOnlyMatchingTodos() throws Exception {
        mockMvc.perform(get("/api/todos?done=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].done").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    void deleteTodoReturns204() throws Exception {
        String response = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"To be deleted\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/todos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/todos/" + id))
                .andExpect(status().isNotFound());
    }
}
