package com.clinic.app.common.exception;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.Ordered;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ✅ 404 / 400 
  @ExceptionHandler(ErrorResponseException.class)
  ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest req) {
    ProblemDetail pd = ex.getBody();
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 422 - Validación @Valid en DTOs (body JSON)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {

    ProblemDetail pd = ProblemDetail.forStatus(422);
    pd.setType(URI.create("https://clinic.com/problems/validation"));
    pd.setTitle("Validation failed");
    pd.setDetail("One or more fields are invalid");
    pd.setInstance(URI.create(req.getRequestURI()));

    Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors
          .computeIfAbsent(fe.getField(), k -> new ArrayList<>())
          .add(fe.getDefaultMessage());
    }

    Map<String, Object> errors = new LinkedHashMap<>();
    errors.put("fields", fieldErrors);

    pd.setProperty("errors", errors);
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 422 - Validación por @RequestParam / @PathVariable (ConstraintViolation)
  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(422);
    pd.setType(URI.create("https://clinic.com/problems/validation"));
    pd.setTitle("Validation failed");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 409 - Conflictos de dominio (invitación duplicada, etc.)
  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setType(URI.create("https://clinic.com/problems/conflict"));
    pd.setTitle("Conflict");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 404 - No existe
  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setType(URI.create("https://clinic.com/problems/not-found"));
    pd.setTitle("Not found");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 409 - Violación de constraints de BD (unique, fk, etc.)
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setType(URI.create("https://clinic.com/problems/db"));
    pd.setTitle("Data integrity violation");
    pd.setDetail("The operation violates a database constraint.");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 403 - Forbidden
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    pd.setType(URI.create("https://clinic.com/problems/forbidden"));
    pd.setTitle("Forbidden");
    pd.setDetail("You don't have permission to access this resource.");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 401 - Unauthorized
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    pd.setType(URI.create("https://clinic.com/problems/unauthorized"));
    pd.setTitle("Unauthorized");
    pd.setDetail("Authentication required.");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  // ✅ 500 - Última red de seguridad
  @ExceptionHandler(Exception.class)
  ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setType(URI.create("https://clinic.com/problems/internal"));
    pd.setTitle("Internal server error");
    pd.setDetail("Unexpected error");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));
    return pd;
  }

  private String getTraceId(HttpServletRequest req) {
    Object trace = req.getAttribute("traceId");
    return trace != null ? trace.toString() : null;
  }
}