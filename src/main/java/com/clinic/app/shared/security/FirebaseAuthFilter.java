package com.clinic.app.shared.security;

import com.clinic.app.users.service.UserRoleResolver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifies Firebase ID tokens and populates Spring SecurityContext.
 * Requires FirebaseAuth bean (configured in FirebaseConfig).
 */
@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

  private final FirebaseAuth firebaseAuth;
  private final UserRoleResolver userRoleResolver;

  public FirebaseAuthFilter(FirebaseAuth firebaseAuth, UserRoleResolver userRoleResolver) {
    this.firebaseAuth = firebaseAuth;
    this.userRoleResolver = userRoleResolver;
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
        // If you want to allow non-email providers, you'd need another strategy.
        unauthorized(response, "Invalid token: missing email");
        return;
      }

      // Resolve role using your existing resolver (DB-based)
      var role = userRoleResolver.resolveRole(uid, email);

      // Principal now contains uid + email + role (useful for invitations)
      FirebasePrincipal principal = new FirebasePrincipal(uid, email, role);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );

      // Optional: store request details for logs/audits
      authentication.setDetails(new RequestDetails(
          request.getRemoteAddr(),
          request.getHeader("User-Agent")
      ));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);

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