package com.tbaar.eurder.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    private static final List<String> KEYS = Arrays.stream(ErrorCode.values()).map(ErrorCode::key).toList();

    @Test
    void givenAllErrorCodes_whenReadingTheirKeys_thenEachKeyIsUnique() {
        assertThat(KEYS).doesNotHaveDuplicates();
    }

    @Test
    void givenAllErrorCodes_whenReadingTheirKeys_thenTheyFollowTheKebabCaseFormat() {
        assertThat(KEYS).allMatch(key -> key.matches("(error\\.[a-z]+|validation)\\.[a-z]+(-[a-z]+)*"),
                "is error.<feature>.<problem> or validation.<problem> in kebab-case");
    }

    @Test
    void givenAllErrorCodes_whenNestingTheirKeys_thenNoKeyIsTheParentOfAnother() {
        assertThat(KEYS).noneMatch(key -> KEYS.stream().anyMatch(other -> other.startsWith(key + ".")));
    }
}
