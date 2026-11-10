package com.tbaar.eurder.common.exception;

import java.util.Map;

public interface HasErrorCode {

    ErrorCode code();

    Map<String, Object> params();
}
