package com.tbaar.eurder.feature.security.service;

import java.util.Optional;
import java.util.UUID;

public interface DisplayNames {

    Optional<DisplayName> findByCredentialsId(UUID credentialsId);
}
