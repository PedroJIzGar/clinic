package com.clinic.app.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

  public FirebaseAuthFilter(FirebaseAuth firebaseAuth, UserProvisioningService userProvisioningService) {
    this.firebaseAuth = firebaseAuth;
    this.userProvisioningService = userProvisioningService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String idToken = authHeader.substring("Bearer ".length()).trim();
    if (idToken.isEmpty()) {
      unauthorized(response, "Missing bearer token");
      return;
    }

    try {
      FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);

      String uid = decoded.getUid();
      String email = decoded.getEmail();

      if (uid == null || uid.isBlank()) {
        unauthorized(response, "Invalid token: missing uid");
        return;
      }
      if (email == null || email.isBlank()) {
        // Your DB requires email NOT NULL + UNIQUE (AppUser.email), so we enforce it.
        unauthorized(response, "Invalid token: missing email");
        return;
      }

      // ✅ Provision/update local user in DB (PATIENT by default if new)
      AppUser user = userProvisioningService.provisionOnLogin(uid, email);

      FirebasePrincipal principal = new FirebasePrincipal(user.getFirebaseUid(), user.getEmail(), user.getRole());

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
          );

      authentication.setDetails(new RequestDetails(request.getRemoteAddr(), request.getHeader("User-Agent")));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);

    } catch (UserProvisioningService.BadRequest e) {
      SecurityContextHolder.clearContext();
      unauthorized(response, e.getMessage());
    } catch (UserProvisioningService.Conflict e) {
      SecurityContextHolder.clearContext();
      unauthorized(response, "Account conflict");
    } catch (Exception ex) {
      SecurityContextHolder.clearContext();
      unauthorized(response, "Invalid or expired token");
    }
  }

  private void unauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"status\":401,\"message\":\"" + escapeJson(message) + "\"}");
  }

  private String escapeJson(String s) {
    return s == null ? "" : s.replace("\"", "\\\"");
  }

  public record RequestDetails(String ip, String userAgent) {}
}