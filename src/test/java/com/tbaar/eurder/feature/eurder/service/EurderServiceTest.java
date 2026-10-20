package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.eurder.api.EurderMapper;
import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EurderServiceTest {

    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");
    private static final Clock NEW_YEARS_EVE = Clock.fixed(ZonedDateTime.of(2026, 12, 31, 23, 30, 0, 0, BRUSSELS).toInstant(), BRUSSELS);
    private static final UUID CREDENTIALS_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();

    @Mock
    private EurderRepository eurderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ItemRepository itemRepository;

    private EurderService eurderService;

    @BeforeEach
    void createService() {
        eurderService = new EurderService(eurderRepository, customerRepository, itemRepository, new EurderMapper(), NEW_YEARS_EVE);
    }

    @Test
    void givenItemsInAndOutOfStock_whenOrdering_thenShipsEachItemGroupOnItsOwnDateAndReturnsTheTotal() {
        givenTheCustomerExists();
        Item chair = givenItem(CHAIR_ID, "Chair", "49.99", 10);
        Item table = givenItem(TABLE_ID, "Table", "199.00", 0);
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EurderDto eurder = eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 3), new ItemGroupRequestDto(TABLE_ID, 1)));

        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::itemName, ItemGroupDto::shippingDate)
                .containsExactly(
                        tuple("Chair", LocalDate.of(2027, 1, 1)),
                        tuple("Table", LocalDate.of(2027, 1, 7)));
        assertThat(eurder.totalPrice()).isEqualTo(new BigDecimal("348.97"));
        assertThat(chair.getStock()).isEqualTo(7);
        assertThat(table.getStock()).isZero();
    }

    @Test
    void givenValidOrder_whenOrdering_thenStampsTheEurderWithTheCurrentInstantOfTheClock() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 10);
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 1)));

        ArgumentCaptor<Eurder> saved = ArgumentCaptor.forClass(Eurder.class);
        verify(eurderRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderedAt()).isEqualTo(NEW_YEARS_EVE.instant());
    }

    @Test
    void givenTheSameItemTwice_whenOrdering_thenTheSecondItemGroupSeesTheStockLeftByTheFirst() {
        givenTheCustomerExists();
        Item chair = givenItem(CHAIR_ID, "Chair", "49.99", 3);
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EurderDto eurder = eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 2), new ItemGroupRequestDto(CHAIR_ID, 2)));

        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::shippingDate)
                .containsExactly(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 7));
        assertThat(chair.getStock()).isZero();
    }

    @Test
    void givenAnUnknownItem_whenOrdering_thenThrowsItemNotFoundWithItsIdAndSavesNothing() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 10);
        UUID unknownId = UUID.randomUUID();
        when(itemRepository.findById(unknownId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> eurderService.order(CREDENTIALS_ID,
                request(new ItemGroupRequestDto(CHAIR_ID, 1), new ItemGroupRequestDto(unknownId, 1))));

        assertThat(thrown)
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("No item found for id " + unknownId);
        verify(eurderRepository, never()).save(any(Eurder.class));
    }

    @Test
    void givenCredentialsWithoutCustomer_whenOrdering_thenFailsAsAnInconsistentState() {
        when(customerRepository.findByCredentialsId(CREDENTIALS_ID)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 1))));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No customer linked to credentials " + CREDENTIALS_ID);
    }

    private void givenTheCustomerExists() {
        Customer jane = new Customer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER), "Jane", "Doe",
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
        when(customerRepository.findByCredentialsId(CREDENTIALS_ID)).thenReturn(Optional.of(jane));
    }

    private Item givenItem(UUID id, String name, String price, int stock) {
        Item item = new Item(name, name + " description", new BigDecimal(price), stock);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        return item;
    }

    private static CreateEurderDto request(ItemGroupRequestDto... itemGroups) {
        return new CreateEurderDto(List.of(itemGroups));
    }
}
