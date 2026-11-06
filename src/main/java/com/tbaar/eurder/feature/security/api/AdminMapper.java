package com.tbaar.eurder.feature.security.api;

import com.tbaar.eurder.feature.security.api.dto.AdminDto;
import com.tbaar.eurder.feature.security.domain.Credentials;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {
    public AdminDto toDto(Credentials adminCredentials) {
        return new AdminDto(adminCredentials.getId(), adminCredentials.getEmail());
    }
}
