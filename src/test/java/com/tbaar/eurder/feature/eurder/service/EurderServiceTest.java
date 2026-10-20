package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.customer.api.AddressMapper;
import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.eurder.api.EurderMapper;
import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingItemGroupDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.eurder.domain.ItemGroupToShip;
import com.tbaar.eurder.feature.eurder.exception.EurderNotFoundException;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemUnit;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private ItemUnitRepository itemUnitRepository;

    private EurderService eurderService;

    @BeforeEach
    void createService() {
        eurderService = new EurderService(eurderRepository, customerRepository, itemRepository, itemUnitRepository,
                new EurderMapper(new AddressMapper()), NEW_YEARS_EVE);
    }

    @Test
    void givenItemsWithAndWithoutAvailableUnits_whenOrdering_thenShipsEachItemGroupOnItsOwnDateAndReturnsTheTotal() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 10);
        givenItem(TABLE_ID, "Table", "199.00", 0);
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EurderDto eurder = eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 3), new ItemGroupRequestDto(TABLE_ID, 1)));

        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::itemName, ItemGroupDto::itemState, ItemGroupDto::shippingDate)
                .containsExactly(
                        tuple("Chair", ItemState.PRISTINE, LocalDate.of(2027, 1, 1)),
                        tuple("Table", ItemState.PRISTINE, LocalDate.of(2027, 1, 7)));
        assertThat(eurder.totalPrice()).isEqualTo(new BigDecimal("348.97"));
    }

    @Test
    void givenOnlyPristineUnits_whenOrderingADamagedUnit_thenChargesTheDiscountedPriceAndShipsNextWeek() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 10);
        when(itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(eq(CHAIR_ID), eq(ItemState.DAMAGED), any(Limit.class))).thenReturn(List.of());
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EurderDto eurder = eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 1, ItemState.DAMAGED)));

        assertThat(eurder.itemGroups()).singleElement().satisfies(itemGroup -> {
            assertThat(itemGroup.itemState()).isEqualTo(ItemState.DAMAGED);
            assertThat(itemGroup.itemPrice()).isEqualTo(new BigDecimal("44.99"));
            assertThat(itemGroup.shippingDate()).isEqualTo(LocalDate.of(2027, 1, 7));
        });
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
    void givenTheSameItemTwice_whenOrdering_thenTheSecondItemGroupSeesTheUnitsLeftByTheFirst() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 3);
        when(eurderRepository.save(any(Eurder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EurderDto eurder = eurderService.order(CREDENTIALS_ID, request(new ItemGroupRequestDto(CHAIR_ID, 2), new ItemGroupRequestDto(CHAIR_ID, 2)));

        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::shippingDate)
                .containsExactly(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 7));
        verify(itemUnitRepository).findByItemIdAndStateAndItemGroupIdIsNull(CHAIR_ID, ItemState.PRISTINE, Limit.of(4));
    }

    @Test
    void givenAnUnknownItem_whenOrdering_thenThrowsItemNotFoundWithItsIdAndSavesNothing() {
        givenTheCustomerExists();
        givenItem(CHAIR_ID, "Chair", "49.99", 10);
        UUID unknownId = UUID.randomUUID();
        when(itemRepository.findLockedById(unknownId)).thenReturn(Optional.empty());

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

    @Test
    void givenEurdersOfTheCustomer_whenFindingTheReport_thenReturnsThemWithTheTotalOfAllEurders() {
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        when(eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(CREDENTIALS_ID)).thenReturn(List.of(
                new Eurder(aCustomer(), List.of(ItemGroup.of(chair, ItemState.PRISTINE, 2, 10, LocalDate.of(2026, 12, 30))), NEW_YEARS_EVE.instant()),
                new Eurder(aCustomer(), List.of(ItemGroup.of(chair, ItemState.PRISTINE, 1, 10, LocalDate.of(2026, 12, 31))), NEW_YEARS_EVE.instant())));

        ReportDto report = eurderService.findReport(CREDENTIALS_ID);

        assertThat(report.eurders()).extracting(EurderReportDto::totalPrice)
                .containsExactly(new BigDecimal("99.98"), new BigDecimal("49.99"));
        assertThat(report.totalPrice()).isEqualTo(new BigDecimal("149.97"));
    }

    @Test
    void givenItemGroupsShippingOnTheDateOfTheClock_whenFindingWhatShipsToday_thenReturnsThemGroupedByEurder() {
        UUID eurderId = UUID.randomUUID();
        Address address = new Address("Rue Haute", "1", "1000", "Bruxelles");
        when(eurderRepository.findItemGroupsShippingOn(LocalDate.of(2026, 12, 31))).thenReturn(List.of(
                new ItemGroupToShip(eurderId, "Jane", "Doe", address, "Chair", ItemState.PRISTINE, 3),
                new ItemGroupToShip(eurderId, "Jane", "Doe", address, "Lamp", ItemState.PRISTINE, 1)));

        List<ShippingEurderDto> shipping = eurderService.findShippingToday();

        assertThat(shipping).singleElement().satisfies(eurder -> {
            assertThat(eurder.eurderId()).isEqualTo(eurderId);
            assertThat(eurder.itemGroups()).extracting(ShippingItemGroupDto::itemName).containsExactly("Chair", "Lamp");
        });
    }

    @Test
    void givenAnEurderThatIsNotAmongTheCustomersEurders_whenReordering_thenThrowsEurderNotFoundAndOrdersNothing() {
        UUID eurderId = UUID.randomUUID();
        when(eurderRepository.findByIdAndCustomerCredentialsId(eurderId, CREDENTIALS_ID)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> eurderService.reorder(CREDENTIALS_ID, eurderId));

        assertThat(thrown)
                .isInstanceOf(EurderNotFoundException.class)
                .hasMessage("No eurder found for id " + eurderId);
        verify(eurderRepository, never()).save(any(Eurder.class));
    }

    private void givenTheCustomerExists() {
        when(customerRepository.findByCredentialsId(CREDENTIALS_ID)).thenReturn(Optional.of(aCustomer()));
    }

    private static Customer aCustomer() {
        return new Customer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER), "Jane", "Doe",
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }

    private void givenItem(UUID id, String name, String price, int availablePristineUnits) {
        Item item = new Item(name, name + " description", new BigDecimal(price));
        List<ItemUnit> units = item.newUnits(ItemState.PRISTINE, availablePristineUnits);
        when(itemRepository.findLockedById(id)).thenReturn(Optional.of(item));
        lenient().when(itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(eq(id), eq(ItemState.PRISTINE), any(Limit.class)))
                .thenAnswer(invocation -> units.subList(0, Math.min(units.size(), invocation.<Limit>getArgument(2).max())));
    }

    private static CreateEurderDto request(ItemGroupRequestDto... itemGroups) {
        return new CreateEurderDto(List.of(itemGroups));
    }
}
