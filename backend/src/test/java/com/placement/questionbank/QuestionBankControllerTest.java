package com.placement.questionbank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.config.SecurityConfig;
import com.placement.exception.ResourceNotFoundException;
import com.placement.questionbank.dto.QuestionRequest;
import com.placement.questionbank.dto.QuestionResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for QuestionBankController.
 *
 * TC-QC-01  GET  /api/questions/{id}     STUDENT         → 200
 * TC-QC-02  GET  /api/questions/{id}     unauthenticated → 401/403
 * TC-QC-03  POST /api/questions          FACULTY         → 201
 * TC-QC-04  POST /api/questions          STUDENT         → 403
 * TC-QC-05  DELETE /api/questions/{id}   DEPUTY_TPO      → 204
 * TC-QC-06  GET  /api/questions/{id}     unknown id      → 404
 */
@WebMvcTest(QuestionBankController.class)
@Import(SecurityConfig.class)
@DisplayName("QuestionBankController MockMvc Tests")
class QuestionBankControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private QuestionBankService questionBankService;
    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private QuestionResponse sampleResponse() {
        return QuestionResponse.builder()
                .id(1L)
                .questionText("Explain the JVM memory model")
                .topic("Java")
                .company("Google")
                .difficulty(InterviewQuestion.Difficulty.MEDIUM)
                .questionType(InterviewQuestion.QuestionType.TECHNICAL)
                .createdBy("tpo@college.edu")
                .build();
    }

    private QuestionRequest validRequest() {
        return QuestionRequest.builder()
                .questionText("Explain the JVM memory model")
                .topic("Java")
                .company("Google")
                .targetRole("Software Engineer")
                .difficulty(InterviewQuestion.Difficulty.MEDIUM)
                .questionType(InterviewQuestion.QuestionType.TECHNICAL)
                .tags("jvm,memory")
                .build();
    }

    // ── TC-QC-01 ──────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-QC-01: STUDENT GET /api/questions/1 → 200 with question")
    void getById_student_returns200() throws Exception {
        when(questionBankService.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.questionText").value("Explain the JVM memory model"))
                .andExpect(jsonPath("$.topic").value("Java"));
    }

    // ── TC-QC-02 ──────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-QC-02: Unauthenticated GET /api/questions/1 → 401/403")
    void getById_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/questions/1"))
                .andExpect(status().is4xxClientError());
    }

    // ── TC-QC-03 ──────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "faculty@college.edu", roles = "FACULTY")
    @DisplayName("TC-QC-03: FACULTY POST /api/questions → 201 Created")
    void create_faculty_returns201() throws Exception {
        when(questionBankService.create(any(QuestionRequest.class), anyString()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.createdBy").value("tpo@college.edu"));
    }

    // ── TC-QC-04 ──────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-QC-04: STUDENT POST /api/questions → 403 Forbidden")
    void create_student_returns403() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── TC-QC-05 ──────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "DEPUTY_TPO")
    @DisplayName("TC-QC-05: DEPUTY_TPO DELETE /api/questions/1 → 204 No Content")
    void delete_deputyTpo_returns204() throws Exception {
        doNothing().when(questionBankService).delete(1L);

        mockMvc.perform(delete("/api/questions/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(questionBankService).delete(1L);
    }

    // ── TC-QC-06 ──────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-QC-06: GET /api/questions/999 (not found) → 404")
    void getById_notFound_returns404() throws Exception {
        when(questionBankService.getById(999L))
                .thenThrow(new ResourceNotFoundException("InterviewQuestion not found with id: 999"));

        mockMvc.perform(get("/api/questions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("999")));
    }
}
