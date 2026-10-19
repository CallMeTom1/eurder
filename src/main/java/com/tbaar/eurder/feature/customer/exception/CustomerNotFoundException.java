package com.tbaar.eurder.feature.customer.exception;

import com.tbaar.eurder.common.exception.NotFoundException;

import java.util.UUID;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(UUID id) {
        super("No customer found for id " + id);
    }
}
