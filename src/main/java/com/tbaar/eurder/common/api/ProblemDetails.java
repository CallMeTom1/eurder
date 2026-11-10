package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.util.Map;

public final class ProblemDetails {

    public static final String CODE = "code";
    public static final String PARAMS = "params";
    public static final String ERRORS = "errors";

    private ProblemDetails() {
    }

    public static ProblemDetail problem(HttpStatusCode status, String detail, ErrorCode code, Map<String, Object> params) {
        return withCode(ProblemDetail.forStatusAndDetail(status, detail), code, params);
    }

    static ProblemDetail withCode(ProblemDetail problem, ErrorCode code, Map<String, Object> params) {
        problem.setProperty(CODE, code.key());
        problem.setProperty(PARAMS, params);
        return problem;
    }

    static boolean hasCode(ProblemDetail problem) {
        return problem.getProperties() != null && problem.getProperties().containsKey(CODE);
    }
}
