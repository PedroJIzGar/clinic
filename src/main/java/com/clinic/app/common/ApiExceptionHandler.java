package com.clinic.app.common;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
    return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage(), req.getRequestURI(), "validation");
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleConflict(IllegalStateException ex, HttpServletRequest req) {
    return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req.getRequestURI(), "state");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    return problem(HttpStatus.CONFLICT, "Data integrity violation",
        "The operation violates a database constraint.", req.getRequestURI(), "db");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.BAD_REQUEST, "Validation error",
        "One or more fields are invalid.", req.getRequestURI(), "validation");

    var errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(java.util.stream.Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
            (a, b) -> a
        ));

    pd.setProperty("errors", errors);
    return pd;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
    // En prod aquí NO expondría ex.getMessage()
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
        "Unexpected error.", req.getRequestURI(), "generic");
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail, String instance, String type) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    pd.setType(URI.create("https://clinic.com/problems/" + type));
    pd.setInstance(URI.create(instance));
    return pd;
  }
}