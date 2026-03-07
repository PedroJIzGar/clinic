package com.clinic.app.shared.config;

import java.io.IOException;

import com.clinic.app.shared.exception.ApiProblemFactory;
import com.clinic.app.shared.security.FirebaseAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public AuthenticationEntryPoint restAuthenticationEntryPoint(
      ObjectMapper objectMapper,
      ApiProblemFactory problemFactory) {
    return (request, response, authException) -> {
      ProblemDetail pd = problemFactory.build(
          HttpStatus.UNAUTHORIZED,
          "https://clinic.com/problems/unauthorized",
          "Unauthorized",
          "Authentication required.",
          request);
      writeProblem(response, objectMapper, pd);
    };
  }

  @Bean
  public AccessDeniedHandler restAccessDeniedHandler(
      ObjectMapper objectMapper,
      ApiProblemFactory problemFactory) {
    return (request, response, accessDeniedException) -> {
      ProblemDetail pd = problemFactory.build(
          HttpStatus.FORBIDDEN,
          "https://clinic.com/problems/forbidden",
          "Forbidden",
          "You don't have permission to access this resource.",
          request);
      writeProblem(response, objectMapper, pd);
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      FirebaseAuthFilter firebaseFilter,
      AuthenticationEntryPoint restAuthenticationEntryPoint,
      AccessDeniedHandler restAccessDeniedHandler) throws Exception {

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(restAuthenticationEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/api/public/invitations/verify").permitAll()
            .requestMatchers("/api/public/invitations/accept").authenticated()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private static void writeProblem(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      ProblemDetail pd) throws IOException {
    response.setStatus(pd.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), pd);
  }
}