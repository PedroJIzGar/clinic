package com.clinic.app.shared.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.service.UserProvisioningService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

  private final FirebaseAuth firebaseAuth;
  private final UserProvisioningService userProvisioningService;
  private final AuthenticationEntryPoint authenticationEntryPoint;

  public FirebaseAuthFilter(
      FirebaseAuth firebaseAuth,
      UserProvisioningService userProvisioningService,
      AuthenticationEntryPoint restAuthenticationEntryPoint) {
    this.firebaseAuth = firebaseAuth;
    this.userProvisioningService = userProvisioningService;
    this.authenticationEntryPoint = restAuthenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String idToken = authHeader.substring("Bearer ".length()).trim();
    if (idToken.isEmpty()) {
      failAuth(request, response, "Missing bearer token");
      return;
    }

    boolean mdcPopulated = false;

    try {
      FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);

      String uid = decoded.getUid();
      String email = decoded.getEmail();

      if (uid == null || uid.isBlank()) {
        failAuth(request, response, "Invalid token: missing uid");
        return;
      }
      if (email == null || email.isBlank()) {
        // AppUser.email is NOT NULL + UNIQUE
        failAuth(request, response, "Invalid token: missing email");
        return;
      }

      // ✅ Provision/update local user
      AppUser user = userProvisioningService.provisionOnLogin(uid, email);

      // ✅ Add user context to logs (traceId already set by TraceIdFilter)
      MDC.put("uid", user.getFirebaseUid());
      MDC.put("email", user.getEmail());
      MDC.put("role", user.getRole().name());
      mdcPopulated = true;

      FirebasePrincipal principal = new FirebasePrincipal(
          user.getFirebaseUid(),
          user.getEmail(),
          user.getRole());

      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
          principal,
          null,
          List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);

    } catch (IllegalArgumentException e) {
      failAuth(request, response, e.getMessage());
    } catch (com.clinic.app.shared.exception.ConflictException e) {
      // Keep generic to avoid leaking account state
      failAuth(request, response, "Account conflict");
    } catch (Exception ex) {
      failAuth(request, response, "Invalid or expired token");
    } finally {
      if (mdcPopulated) {
        MDC.remove("uid");
        MDC.remove("email");
        MDC.remove("role");
      }
    }
  }

  private void failAuth(HttpServletRequest request, HttpServletResponse response, String message)
      throws IOException, ServletException {
    SecurityContextHolder.clearContext();
    authenticationEntryPoint.commence(
        request,
        response,
        new InsufficientAuthenticationException(message));
  }
}