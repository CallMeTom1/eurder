package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.customer.service.CustomerService;
import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.security.domain.CredentialsRepository;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class EurderServiceIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock newYearsEveClock() {
            ZoneId brussels = ZoneId.of("Europe/Brussels");
            return Clock.fixed(ZonedDateTime.of(2026, 12, 31, 10, 0, 0, 0, brussels).toInstant(), brussels);
        }
    }

    @Autowired
    private EurderService eurderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EurderRepository eurderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID janeCredentialsId;

    @BeforeEach
    void registerJane() {
        customerService.register(new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456"));
        janeCredentialsId = credentialsRepository.findByEmail("jane@doe.be").orElseThrow().getId();
    }

    @AfterEach
    void deleteCreatedData() {
        eurderRepository.deleteAll();
        itemRepository.deleteAll();
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenItemsInAndOutOfStock_whenOrdering_thenStoresTheEurderAndTheNewStockInTheDatabase() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        Item table = itemRepository.save(new Item("Table", "Oak table", new BigDecimal("199.00"), 0));

        EurderDto eurder = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(table.getId(), 1))));

        assertThat(eurderRepository.existsById(eurder.id())).isTrue();
        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::itemId, ItemGroupDto::shippingDate)
                .containsExactly(
                        tuple(chair.getId(), LocalDate.of(2027, 1, 1)),
                        tuple(table.getId(), LocalDate.of(2027, 1, 7)));
        assertThat(itemRepository.findById(chair.getId()).orElseThrow().getStock()).isEqualTo(7);
        assertThat(itemRepository.findById(table.getId()).orElseThrow().getStock()).isZero();
    }

    @Test
    void givenAnUnknownItemAfterAValidOne_whenOrdering_thenNothingIsStoredAndNoStockMoves() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        UUID unknownId = UUID.randomUUID();

        Throwable thrown = catchThrowable(() -> eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(unknownId, 1)))));

        assertThat(thrown).isInstanceOf(ItemNotFoundException.class);
        assertThat(eurderRepository.count()).isZero();
        assertThat(itemRepository.findById(chair.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    void givenTheStockChangedByAnotherTransactionMeanwhile_whenTheEurderCommits_thenFailsAndKeepsTheOtherChange() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        TransactionTemplate eurderTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Throwable thrown = catchThrowable(() -> eurderTransaction.executeWithoutResult(eurderStatus -> {
            eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 3))));
            concurrentTransaction.executeWithoutResult(concurrentStatus ->
                    itemRepository.findById(chair.getId()).orElseThrow().removeFromStock(4));
        }));

        assertThat(thrown).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(eurderRepository.count()).isZero();
        assertThat(itemRepository.findById(chair.getId()).orElseThrow().getStock()).isEqualTo(6);
    }
}
