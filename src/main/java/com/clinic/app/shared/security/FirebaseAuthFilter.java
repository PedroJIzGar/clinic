package com.clinic.app.shared.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.clinic.app.users.domain.Role;
import com.clinic.app.users.service.UserRoleResolver;
import com.google.common.net.HttpHeaders;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FirebaseAuthFilter extends OncePerRequestFilter {
	
	private final UserRoleResolver roleResolver;
	
	public FirebaseAuthFilter(UserRoleResolver roleResolver) {
		this.roleResolver = roleResolver;
	}
	
	@Override
	protected void doFilterInternal (HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
	throws ServletException, IOException {
		
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		
		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
	
		String token = authHeader.substring("Bearer ".length()).trim();
		
		try {
			FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
			String uid = decoded.getUid();
			String email = decoded.getEmail();
			
			Role role = roleResolver.resolveRole(uid, email);
			
			var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
			var authentication = new UsernamePasswordAuthenticationToken(uid, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		}catch (Exception ex) {
			  SecurityContextHolder.clearContext();
			  response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			  response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
			  response.getWriter().write("""
			    {"type":"https://clinic.com/problems/auth","title":"Unauthorized","status":401,
			     "detail":"Missing or invalid token","instance":"%s"}
			    """.formatted(request.getRequestURI()));
			  return;
		}
	}

}
