package com.tbaar.eurder.feature.security.exception;

import com.tbaar.eurder.common.exception.ForbiddenException;
import com.tbaar.eurder.feature.security.domain.Feature;

public class FeatureAccessDeniedException extends ForbiddenException {

    public FeatureAccessDeniedException(String email, Feature feature) {
        super("User " + email + " has no access to " + feature);
    }
}
