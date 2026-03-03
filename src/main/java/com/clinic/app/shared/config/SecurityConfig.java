package com.clinic.app.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.clinic.app.shared.security.FirebaseAuthFilter;
import com.clinic.app.users.service.UserRoleResolver;
import com.google.firebase.auth.FirebaseAuth;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      FirebaseAuth firebaseAuth,
      UserRoleResolver roleResolver
  ) throws Exception {

    // ✅ now injected dependencies (no static getInstance in filter)
    FirebaseAuthFilter firebaseFilter = new FirebaseAuthFilter(firebaseAuth, roleResolver);

    AuthenticationEntryPoint entryPoint = (request, response, authException) -> {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("""
        {"type":"https://clinic.com/problems/auth","title":"Unauthorized","status":401,
         "detail":"Missing or invalid token","instance":"%s"}
        """.formatted(request.getRequestURI()));
    };

    AccessDeniedHandler deniedHandler = (request, response, accessDeniedException) -> {
      response.setStatus(403);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("""
        {"type":"https://clinic.com/problems/authz","title":"Forbidden","status":403,
         "detail":"You don't have permission to access this resource","instance":"%s"}
        """.formatted(request.getRequestURI()));
    };

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(entryPoint)
            .accessDeniedHandler(deniedHandler)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/public/invitations/verify").permitAll()
            .requestMatchers("/api/public/invitations/accept").authenticated()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}