package com.interniq.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interniq.exception.GlobalExceptionHandler;
import com.interniq.task.dto.TaskRequest;
import com.interniq.task.dto.TaskResponse;
import com.interniq.user.Role;
import com.interniq.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest {

    private final TaskService taskService = Mockito.mock(TaskService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createTaskReturnsCreatedTask() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Build auth");
        request.setAssignedToInternId(10L);
        request.setPriority(Priority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(5));

        when(taskService.createTask(any(TaskRequest.class), any())).thenReturn(TaskResponse.builder()
                .id(50L)
                .title("Build auth")
                .status(TaskStatus.PENDING)
                .priority(Priority.HIGH)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/api/tasks")
                        .principal(new UsernamePasswordAuthenticationToken(user(), null, user().getAuthorities()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createTaskValidationReturnsBadRequest() throws Exception {
        TaskRequest request = new TaskRequest();

        mockMvc.perform(post("/api/tasks")
                        .principal(new UsernamePasswordAuthenticationToken(user(), null, user().getAuthorities()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.assignedToInternId").exists());
    }

    private User user() {
        return User.builder()
                .id(2L)
                .name("Manager")
                .email("manager@test.com")
                .password("encoded")
                .role(Role.MANAGER)
                .active(true)
                .build();
    }
}
