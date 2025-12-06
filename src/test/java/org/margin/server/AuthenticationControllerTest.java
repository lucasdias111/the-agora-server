package org.margin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.margin.server.authentication.controllers.AuthenticationController;
import org.margin.server.authentication.models.AuthResponse;
import org.margin.server.authentication.services.AuthenticationService;
import org.margin.server.config.SecurityConfig;
import org.margin.server.config.TestSecurityConfig;
import org.margin.server.config.filters.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(TestSecurityConfig.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authService;

    @Test
    void login_WithValidCredentials_ReturnsAuthResponse() throws Exception {
        AuthResponse mockResponse = new AuthResponse(true, "Login successful", "user", "token123");
        when(authService.authenticateUser("testuser", "password123"))
                .thenReturn(mockResponse);

        var request = new AuthenticationController.LoginRequest("testuser", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.username").value("user"));

        verify(authService).authenticateUser("testuser", "password123");
    }

    @Test
    void login_WithInvalidCredentials_ReturnsAuthResponse() throws Exception {
        AuthResponse mockResponse = new AuthResponse(false, "Invalid credentials", null, null);
        when(authService.authenticateUser("testuser", "wrongpassword"))
                .thenReturn(mockResponse);

        var request = new AuthenticationController.LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(authService).authenticateUser("testuser", "wrongpassword");
    }

    @Test
    void register_WithValidData_ReturnsCreatedAndAuthResponse() throws Exception {
        doNothing().when(authService).registerUser("newuser", "newuser@example.com", "password123");

        AuthResponse mockResponse = new AuthResponse(true, "Registration successful", "newuser", "token456");
        when(authService.authenticateUser("newuser", "password123"))
                .thenReturn(mockResponse);

        var request = new AuthenticationController.RegisterRequest(
                "newuser", "newuser@example.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.token").value("token456"))
                .andExpect(jsonPath("$.username").value("newuser"));

        verify(authService).registerUser("newuser", "newuser@example.com", "password123");
        verify(authService).authenticateUser("newuser", "password123");
    }

    @Test
    void register_WithDuplicateUsername_ReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(authService).registerUser("existinguser", "test@example.com", "password123");

        var request = new AuthenticationController.RegisterRequest(
                "existinguser", "test@example.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(authService).registerUser("existinguser", "test@example.com", "password123");
        verify(authService, never()).authenticateUser(anyString(), anyString());
    }

    @Test
    void register_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Invalid email format"))
                .when(authService).registerUser("testuser", "invalid-email", "password123");

        var request = new AuthenticationController.RegisterRequest(
                "testuser", "invalid-email", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }

    @Test
    void register_WithWeakPassword_ReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Password too weak"))
                .when(authService).registerUser("testuser", "test@example.com", "123");

        var request = new AuthenticationController.RegisterRequest(
                "testuser", "test@example.com", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password too weak"));
    }
}