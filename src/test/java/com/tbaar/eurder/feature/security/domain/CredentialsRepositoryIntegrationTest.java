package com.tbaar.eurder.feature.security.domain;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.common.config.MongoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataMongoTest
@Import(MongoConfig.class)
class CredentialsRepositoryIntegrationTest {

    @Autowired
    private CredentialsRepository credentialsRepository;

    private final String email = UniqueEmail.of("jane");

    @Test
    void givenSavedCredentials_whenFindingByEmail_thenReturnsThem() {
        Credentials saved = credentialsRepository.save(new Credentials(email, "hashed-password", Role.CUSTOMER));

        Optional<Credentials> found = credentialsRepository.findByEmail(email);

        assertThat(found).hasValueSatisfying(credentials -> {
            assertThat(credentials.getId()).isEqualTo(saved.getId());
            assertThat(credentials.getPasswordHash()).isEqualTo("hashed-password");
            assertThat(credentials.getRole()).isEqualTo(Role.CUSTOMER);
        });
    }

    @Test
    void givenCredentialsWithAnEmail_whenSavingOthersWithTheSameEmail_thenTheUniqueIndexRefusesThem() {
        credentialsRepository.save(new Credentials(email, "hashed-password", Role.CUSTOMER));

        Throwable thrown = catchThrowable(() -> credentialsRepository.save(new Credentials(email, "other-password", Role.CUSTOMER)));

        assertThat(thrown).isInstanceOf(DuplicateKeyException.class);
    }
}
