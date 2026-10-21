package com.placement.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.placement.ai.dto.*;
import com.placement.ai.exception.AiServiceException;
import com.placement.config.SecurityConfig;
import com.placement.questionbank.InterviewQuestion;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for AIController.
 * Services are mocked via @MockBean; Spring Security is active via @Import(SecurityConfig).
 *
 * TC-AC-01  POST /resume/analyze  authenticated → 200 + ATS score in body
 * TC-AC-02  POST /resume/analyze  unauthenticated → 401/403
 * TC-AC-03  POST /interview/generate  missing required field → 400 validation error
 * TC-AC-04  POST /interview/generate-and-save  STUDENT role → 403 forbidden
 * TC-AC-05  POST /interview/generate-and-save  TPO role → 201 + savedQuestionIds
 * TC-AC-06  POST /preparation/plan  driveDate in past → 400 validation error
 * TC-AC-07  POST /preparation/plan  AI timeout → 504 Gateway Timeout
 * TC-AC-08  POST /resume/analyze  file > 5MB (extractor throws) → 400
 */
@WebMvcTest(AIController.class)
@Import(SecurityConfig.class)
@DisplayName("AIController MockMvc Tests")
class AIControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ResumeAnalyzer     resumeAnalyzer;
    @MockBean private InterviewGenerator interviewGenerator;
    @MockBean private PreparationPlanner preparationPlanner;

    // Spring Security wiring stubs (needed by SecurityConfig)
    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
            com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private ResumeAnalysisResponse goodResumeResponse() {
        return ResumeAnalysisResponse.builder()
                .atsScore(82)
                .scoreBand("Excellent")
                .matchedKeywords(List.of("Java", "Spring"))
                .missingKeywords(List.of("Kubernetes"))
                .topImprovements(List.of(
                    "Add summary", "Quantify achievements", "Add certs",
                    "GitHub links", "ATS keywords"))
                .fileFormat("PDF")
                .wordCount(450)
                .build();
    }

    private InterviewGenerationResponse goodGenerationResponse(boolean saved) {
        return InterviewGenerationResponse.builder()
                .questions(List.of(
                    InterviewGenerationResponse.GeneratedQuestion.builder()
                        .questionText("What is the JVM memory model?")
                        .answer("Model answer here.")
                        .hint("Think heap vs stack")
                        .followUp("How does GC work?")
                        .relatedSkills(List.of("Java"))
                        .estimatedMinutes(5)
                        .build()))
                .totalGenerated(1)
                .savedToBank(saved)
                .savedQuestionIds(saved ? List.of(42L) : List.of())
                .targetRole("Software Engineer")
                .difficulty(InterviewQuestion.Difficulty.MEDIUM)
                .questionType(InterviewQuestion.QuestionType.TECHNICAL)
                .build();
    }

    private PreparationPlanResponse goodPlanResponse() {
        return PreparationPlanResponse.builder()
                .targetRole("Software Engineer")
                .driveDate(LocalDate.now().plusDays(14))
                .planGeneratedOn(LocalDate.now())
                .totalDaysAvailable(14)
                .summary("Focus on DSA and Java.")
                .skillGaps(List.of("Java", "DSA"))
                .weeklyPlan(List.of())
                .upcomingDays(List.of())
                .build();
    }

    private InterviewGenerationRequest validGenerationRequest() {
        return InterviewGenerationRequest.builder()
                .targetRole("Software Engineer")
                .targetCompany("Google")
                .skills(List.of("Java", "Spring Boot"))
                .difficulty(InterviewQuestion.Difficulty.MEDIUM)
                .questionType(InterviewQuestion.QuestionType.TECHNICAL)
                .count(3)
                .saveToBank(false)
                .build();
    }

    private PreparationPlanRequest validPlanRequest() {
        return PreparationPlanRequest.builder()
                .targetRole("Software Engineer")
                .driveDate(LocalDate.now().plusDays(14))
                .skillGaps(List.of("Java", "DSA"))
                .dailyStudyHours(4)
                .build();
    }

    // ── TC-AC-01 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-01: Authenticated STUDENT uploads resume → 200 + atsScore")
    void analyzeResume_authenticated_returns200() throws Exception {
        when(resumeAnalyzer.analyze(any(), any())).thenReturn(goodResumeResponse());

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf",
                "dummy pdf content".getBytes());

        mockMvc.perform(multipart("/api/ai/resume/analyze")
                        .file(pdfFile)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atsScore").value(82))
                .andExpect(jsonPath("$.scoreBand").value("Excellent"))
                .andExpect(jsonPath("$.matchedKeywords[0]").value("Java"))
                .andExpect(jsonPath("$.topImprovements").isArray());
    }

    // ── TC-AC-02 ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-AC-02: Unauthenticated request to /resume/analyze → 401/403")
    void analyzeResume_unauthenticated_returns401or403() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf",
                "dummy pdf".getBytes());

        mockMvc.perform(multipart("/api/ai/resume/analyze")
                        .file(pdfFile)
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    // ── TC-AC-03 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-03: Missing 'skills' field in generate request → 400 validation")
    void generateQuestions_missingSkills_returns400() throws Exception {
        // skills is @NotEmpty — omit it
        String badBody = """
            {
              "targetRole": "Software Engineer",
              "difficulty": "MEDIUM",
              "questionType": "TECHNICAL",
              "count": 3
            }
            """;

        mockMvc.perform(post("/api/ai/interview/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.skills").exists());
    }

    // ── TC-AC-04 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-04: STUDENT hits /generate-and-save → 403 Forbidden")
    void generateAndSave_studentRole_returns403() throws Exception {
        mockMvc.perform(post("/api/ai/interview/generate-and-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGenerationRequest()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── TC-AC-05 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "tpo@college.edu", roles = "TPO")
    @DisplayName("TC-AC-05: TPO hits /generate-and-save → 201 + savedQuestionIds")
    void generateAndSave_tpoRole_returns201WithIds() throws Exception {
        when(interviewGenerator.generate(any(), anyString()))
                .thenReturn(goodGenerationResponse(true));

        mockMvc.perform(post("/api/ai/interview/generate-and-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validGenerationRequest()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedToBank").value(true))
                .andExpect(jsonPath("$.savedQuestionIds[0]").value(42))
                .andExpect(jsonPath("$.totalGenerated").value(1));
    }

    // ── TC-AC-06 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-06: driveDate in the past → 400 validation error")
    void generatePlan_pastDriveDate_returns400() throws Exception {
        PreparationPlanRequest badRequest = validPlanRequest();
        badRequest.setDriveDate(LocalDate.now().minusDays(1)); // past date

        mockMvc.perform(post("/api/ai/preparation/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.driveDate").exists());
    }

    // ── TC-AC-07 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-07: AI timeout on /preparation/plan → 504 Gateway Timeout")
    void generatePlan_aiTimeout_returns504() throws Exception {
        when(preparationPlanner.plan(any()))
                .thenThrow(new AiServiceException(
                        AiServiceException.AiErrorType.TIMEOUT,
                        "AI provider request timed out after 30s"));

        mockMvc.perform(post("/api/ai/preparation/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPlanRequest()))
                        .with(csrf()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("timed out")));
    }

    // ── TC-AC-08 ───────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("TC-AC-08: Oversized file (extractor throws IllegalArgument) → 400")
    void analyzeResume_oversizedFile_returns400() throws Exception {
        when(resumeAnalyzer.analyze(any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "File size 6144 KB exceeds the 5 MB limit"));

        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "big.pdf", "application/pdf",
                "fake-content".getBytes());

        mockMvc.perform(multipart("/api/ai/resume/analyze")
                        .file(bigFile)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("5 MB")));
    }
}
