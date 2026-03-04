package com.clinic.app.shared.exception;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
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

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ✅ 404 / 400 (Spring's ErrorResponseException family)
  @ExceptionHandler(ErrorResponseException.class)
  ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest req) {
    ProblemDetail pd = ex.getBody();
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("ErrorResponseException status={} path={} traceId={} detail={}",
        pd.getStatus(), req.getRequestURI(), getTraceId(req), safe(pd.getDetail()));

    return pd;
  }

  // ✅ 400 - Validation @Valid on DTOs
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {

    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
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

    log.warn("Validation error path={} traceId={} errors={}",
        req.getRequestURI(), getTraceId(req), fieldErrors);

    return pd;
  }

  // ✅ 400 - Validation for @RequestParam / @PathVariable (ConstraintViolation)
  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setType(URI.create("https://clinic.com/problems/validation"));
    pd.setTitle("Validation failed");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("ConstraintViolation path={} traceId={} detail={}",
        req.getRequestURI(), getTraceId(req), safe(ex.getMessage()));

    return pd;
  }

  // ✅ 409 - Domain conflicts
  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setType(URI.create("https://clinic.com/problems/conflict"));
    pd.setTitle("Conflict");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("Conflict path={} traceId={} detail={}",
        req.getRequestURI(), getTraceId(req), safe(ex.getMessage()));

    return pd;
  }

  // ✅ 404 - Not found
  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setType(URI.create("https://clinic.com/problems/not-found"));
    pd.setTitle("Not found");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("NotFound path={} traceId={} detail={}",
        req.getRequestURI(), getTraceId(req), safe(ex.getMessage()));

    return pd;
  }

  // ✅ 409 - Database constraint violations (unique, fk, etc.)
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setType(URI.create("https://clinic.com/problems/db"));
    pd.setTitle("Data integrity violation");
    pd.setDetail("The operation violates a database constraint.");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("DataIntegrityViolation path={} traceId={} cause={}",
        req.getRequestURI(), getTraceId(req), safe(rootMessage(ex)));

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

    log.warn("AccessDenied path={} traceId={}", req.getRequestURI(), getTraceId(req));

    return pd;
  }

  // ✅ 401 - Unauthorized (note: filter/entrypoint may handle it before reaching here)
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    pd.setType(URI.create("https://clinic.com/problems/unauthorized"));
    pd.setTitle("Unauthorized");
    pd.setDetail("Authentication required.");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("AuthenticationException path={} traceId={}", req.getRequestURI(), getTraceId(req));

    return pd;
  }

  // ✅ 400 - IllegalArgumentException (useful safety net)
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setType(URI.create("https://clinic.com/problems/bad-request"));
    pd.setTitle("Bad request");
    pd.setDetail(ex.getMessage());
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    log.warn("IllegalArgument path={} traceId={} detail={}",
        req.getRequestURI(), getTraceId(req), safe(ex.getMessage()));

    return pd;
  }

  // ✅ 500 - Last resort
  @ExceptionHandler(Exception.class)
  ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setType(URI.create("https://clinic.com/problems/internal"));
    pd.setTitle("Internal server error");
    pd.setDetail("Unexpected error");
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("traceId", getTraceId(req));

    // Here we DO want the stacktrace
    log.error("Unhandled exception path={} traceId={}",
        req.getRequestURI(), getTraceId(req), ex);

    return pd;
  }

  private String getTraceId(HttpServletRequest req) {
    Object trace = req.getAttribute("traceId");
    return trace != null ? trace.toString() : null;
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }

  private String rootMessage(Throwable t) {
    Throwable cur = t;
    while (cur.getCause() != null) cur = cur.getCause();
    return cur.getMessage();
  }
}