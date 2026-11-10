package com.tbaar.eurder.common.api;

import com.tbaar.eurder.common.exception.ErrorCode;
import org.springframework.http.ProblemDetail;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public final class ProblemDetailAssertions {

    private ProblemDetailAssertions() {
    }

    public static void assertCode(ProblemDetail problem, ErrorCode code, Map<String, Object> params) {
        assertThat(problem.getProperties())
                .containsEntry(ProblemDetails.CODE, code.key())
                .containsEntry(ProblemDetails.PARAMS, params);
    }
}
