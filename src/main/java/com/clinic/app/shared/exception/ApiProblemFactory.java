package com.clinic.app.shared.exception;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ApiProblemFactory {

    public ProblemDetail build(
            HttpStatus status,
            String type,
            String title,
            String detail,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(type));
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setInstance(URI.create(request.getRequestURI()));

        Object traceId = request.getAttribute("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId.toString());
        }

        return pd;
    }
}