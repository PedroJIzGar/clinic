package com.clinic.app.shared.exception;

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

  private final ApiProblemFactory problemFactory;

  public GlobalExceptionHandler(ApiProblemFactory problemFactory) {
    this.problemFactory = problemFactory;
  }

  @ExceptionHandler(ErrorResponseException.class)
  ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest req) {
    ProblemDetail pd = ex.getBody();
    pd.setInstance(java.net.URI.create(req.getRequestURI()));

    Object traceId = req.getAttribute("traceId");
    if (traceId != null) {
      pd.setProperty("traceId", traceId.toString());
    }

    log.warn("ErrorResponseException status={} path={} traceId={} detail={}",
        pd.getStatus(),
        req.getRequestURI(),
        getTraceId(req),
        safe(pd.getDetail()));

    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.BAD_REQUEST,
        "https://clinic.com/problems/validation",
        "Validation failed",
        "One or more fields are invalid",
        req);

    Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors
          .computeIfAbsent(fe.getField(), k -> new ArrayList<>())
          .add(fe.getDefaultMessage());
    }

    Map<String, Object> errors = new LinkedHashMap<>();
    errors.put("fields", fieldErrors);

    pd.setProperty("errors", errors);

    log.warn("Validation error path={} traceId={} errors={}",
        req.getRequestURI(),
        getTraceId(req),
        fieldErrors);

    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.BAD_REQUEST,
        "https://clinic.com/problems/validation",
        "Validation failed",
        ex.getMessage(),
        req);

    log.warn("ConstraintViolation path={} traceId={} detail={}",
        req.getRequestURI(),
        getTraceId(req),
        safe(ex.getMessage()));

    return pd;
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.CONFLICT,
        "https://clinic.com/problems/conflict",
        "Conflict",
        ex.getMessage(),
        req);

    log.warn("Conflict path={} traceId={} detail={}",
        req.getRequestURI(),
        getTraceId(req),
        safe(ex.getMessage()));

    return pd;
  }

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.NOT_FOUND,
        "https://clinic.com/problems/not-found",
        "Not found",
        ex.getMessage(),
        req);

    log.warn("NotFound path={} traceId={} detail={}",
        req.getRequestURI(),
        getTraceId(req),
        safe(ex.getMessage()));

    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.CONFLICT,
        "https://clinic.com/problems/db",
        "Data integrity violation",
        "The operation violates a database constraint.",
        req);

    log.warn("DataIntegrityViolation path={} traceId={} cause={}",
        req.getRequestURI(),
        getTraceId(req),
        safe(rootMessage(ex)));

    return pd;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.FORBIDDEN,
        "https://clinic.com/problems/forbidden",
        "Forbidden",
        "You don't have permission to access this resource.",
        req);

    log.warn("AccessDenied path={} traceId={}",
        req.getRequestURI(),
        getTraceId(req));

    return pd;
  }

  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.UNAUTHORIZED,
        "https://clinic.com/problems/unauthorized",
        "Unauthorized",
        "Authentication required.",
        req);

    log.warn("AuthenticationException path={} traceId={}",
        req.getRequestURI(),
        getTraceId(req));

    return pd;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.BAD_REQUEST,
        "https://clinic.com/problems/bad-request",
        "Bad request",
        ex.getMessage(),
        req);

    log.warn("IllegalArgument path={} traceId={} detail={}",
        req.getRequestURI(),
        getTraceId(req),
        safe(ex.getMessage()));

    return pd;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
    ProblemDetail pd = problemFactory.build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "https://clinic.com/problems/internal",
        "Internal server error",
        "Unexpected error",
        req);

    log.error("Unhandled exception path={} traceId={}",
        req.getRequestURI(),
        getTraceId(req),
        ex);

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
    Throwable current = t;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage();
  }
}