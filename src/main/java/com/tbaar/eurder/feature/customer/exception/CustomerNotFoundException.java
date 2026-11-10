package com.tbaar.eurder.feature.customer.exception;

import com.tbaar.eurder.common.exception.ErrorCode;
import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.Map;
import java.util.UUID;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(UUID id) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, Map.of("id", id), "No customer found for id " + id);
    }
}
