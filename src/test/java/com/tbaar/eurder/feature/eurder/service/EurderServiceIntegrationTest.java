package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.common.MutableClock;
import com.tbaar.eurder.feature.customer.api.dto.AddressDto;
import com.tbaar.eurder.feature.customer.api.dto.CreateCustomerDto;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.customer.service.CustomerService;
import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingItemGroupDto;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.eurder.exception.EurderNotFoundException;
import com.tbaar.eurder.feature.item.api.dto.UpdateItemDto;
import com.tbaar.eurder.feature.item.domain.AvailableUnits;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import com.tbaar.eurder.feature.item.service.ItemService;
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

    private static final ZonedDateTime NEW_YEARS_EVE = ZonedDateTime.of(2026, 12, 31, 10, 0, 0, 0, ZoneId.of("Europe/Brussels"));

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NEW_YEARS_EVE);
        }
    }

    @Autowired
    private MutableClock clock;

    @Autowired
    private EurderService eurderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EurderRepository eurderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private UUID janeCredentialsId;

    @BeforeEach
    void registerJane() {
        clock.setTo(NEW_YEARS_EVE);
        customerService.register(new CreateCustomerDto("Jane", "Doe", "jane@doe.be", "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456"));
        janeCredentialsId = credentialsRepository.findByEmail("jane@doe.be").orElseThrow().getId();
    }

    @AfterEach
    void deleteCreatedData() {
        eurderRepository.deleteAll();
        itemUnitRepository.deleteAll();
        itemRepository.deleteAll();
        customerRepository.deleteAll();
        credentialsRepository.deleteAllByRole(Role.CUSTOMER);
    }

    @Test
    void givenItemsInAndOutOfStock_whenOrdering_thenStoresTheEurderAndTheNewStockInTheDatabase() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        Item table = itemWithPristineUnits("Table", "Oak table", "199.00", 0);

        EurderDto eurder = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(table.getId(), 1))));

        assertThat(eurderRepository.existsById(eurder.id())).isTrue();
        assertThat(eurder.itemGroups())
                .extracting(ItemGroupDto::itemId, ItemGroupDto::shippingDate)
                .containsExactly(
                        tuple(chair.getId(), LocalDate.of(2027, 1, 1)),
                        tuple(table.getId(), LocalDate.of(2027, 1, 7)));
        assertThat(availableUnits(chair)).isEqualTo(7);
        assertThat(availableUnits(table)).isZero();
    }

    @Test
    void givenAnUnknownItemAfterAValidOne_whenOrdering_thenNothingIsStoredAndNoStockMoves() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        UUID unknownId = UUID.randomUUID();

        Throwable thrown = catchThrowable(() -> eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(unknownId, 1)))));

        assertThat(thrown).isInstanceOf(ItemNotFoundException.class);
        assertThat(eurderRepository.count()).isZero();
        assertThat(availableUnits(chair)).isEqualTo(10);
    }

    @Test
    void givenAnotherEurderTakingTheSameUnitsMeanwhile_whenTheEurderCommits_thenFailsAndKeepsTheOtherEurder() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        UUID zoe = registerCustomer("zoe@adams.be");
        TransactionTemplate eurderTransaction = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Throwable thrown = catchThrowable(() -> eurderTransaction.executeWithoutResult(eurderStatus -> {
            eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 3))));
            concurrentTransaction.executeWithoutResult(concurrentStatus ->
                    eurderService.order(zoe, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 4)))));
        }));

        assertThat(thrown).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(eurderRepository.count()).isEqualTo(1);
        assertThat(availableUnits(chair)).isEqualTo(6);
    }

    @Test
    void givenAnEurderPlacedBeforeTheItemChanged_whenUpdatingTheItem_thenTheEurderKeepsTheOldNameAndPrice() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        EurderDto eurder = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 2))));

        itemService.update(chair.getId(), new UpdateItemDto("Armchair", "Leather armchair", new BigDecimal("150.00")));

        List<ItemGroup> storedItemGroups = new TransactionTemplate(transactionManager).execute(status ->
                eurderRepository.findById(eurder.id()).orElseThrow().getItemGroups());
        assertThat(storedItemGroups)
                .extracting(ItemGroup::getItemName, ItemGroup::getItemPrice, ItemGroup::subtotal)
                .containsExactly(tuple("Chair", new BigDecimal("49.99"), new BigDecimal("99.98")));
    }

    @Test
    void givenEurdersOfTwoCustomersAndALaterPriceChange_whenFindingTheReport_thenListsOnlyOwnEurdersAtTheCopiedPrices() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 2))));
        eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 1))));
        eurderService.order(registerCustomer("zoe@adams.be"), new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 5))));
        itemService.update(chair.getId(), new UpdateItemDto("Chair", "Wooden chair", new BigDecimal("99.00")));

        ReportDto report = eurderService.findReport(janeCredentialsId);

        assertThat(report.eurders()).extracting(EurderReportDto::totalPrice)
                .containsExactlyInAnyOrder(new BigDecimal("99.98"), new BigDecimal("49.99"));
        assertThat(report.totalPrice()).isEqualTo(new BigDecimal("149.97"));
    }

    @Test
    void givenAnEurderPlacedYesterday_whenFindingWhatShipsToday_thenListsOnlyItsItemGroupInStockWithTheCustomerAddress() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        Item table = itemWithPristineUnits("Table", "Oak table", "199.00", 0);
        EurderDto eurder = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(table.getId(), 1))));
        clock.setTo(NEW_YEARS_EVE.plusDays(1));

        List<ShippingEurderDto> shipping = eurderService.findShippingToday();

        assertThat(shipping).singleElement().satisfies(toShip -> {
            assertThat(toShip.eurderId()).isEqualTo(eurder.id());
            assertThat(toShip.customerName()).isEqualTo("Jane Doe");
            assertThat(toShip.address()).isEqualTo(new AddressDto("Rue Haute", "1", "1000", "Bruxelles"));
            assertThat(toShip.itemGroups()).containsExactly(new ShippingItemGroupDto("Chair", ItemState.PRISTINE, 3));
        });
    }

    @Test
    void givenOwnEurderAndChangesToItsItems_whenReorderingTheNextDay_thenUsesTheCurrentPricesAndStockAndKeepsTheOldEurder() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        Item table = itemWithPristineUnits("Table", "Oak table", "199.00", 0);
        EurderDto original = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(
                new ItemGroupRequestDto(chair.getId(), 3), new ItemGroupRequestDto(table.getId(), 1))));
        itemService.update(chair.getId(), new UpdateItemDto("Chair", "Wooden chair", new BigDecimal("59.99")));
        itemUnitRepository.saveAll(table.newUnits(ItemState.PRISTINE, 5));
        clock.setTo(NEW_YEARS_EVE.plusDays(1));

        EurderDto reordered = eurderService.reorder(janeCredentialsId, original.id());

        assertThat(reordered.id()).isNotEqualTo(original.id());
        assertThat(reordered.itemGroups())
                .extracting(ItemGroupDto::itemId, ItemGroupDto::itemPrice, ItemGroupDto::amount, ItemGroupDto::shippingDate)
                .containsExactly(
                        tuple(chair.getId(), new BigDecimal("59.99"), 3, LocalDate.of(2027, 1, 2)),
                        tuple(table.getId(), new BigDecimal("199.00"), 1, LocalDate.of(2027, 1, 2)));
        assertThat(availableUnits(chair)).isEqualTo(4);
        assertThat(availableUnits(table)).isEqualTo(4);
        List<ItemGroup> originalItemGroups = new TransactionTemplate(transactionManager).execute(status ->
                eurderRepository.findById(original.id()).orElseThrow().getItemGroups());
        assertThat(originalItemGroups)
                .extracting(ItemGroup::getItemPrice, ItemGroup::getShippingDate)
                .containsExactly(
                        tuple(new BigDecimal("49.99"), LocalDate.of(2027, 1, 1)),
                        tuple(new BigDecimal("199.00"), LocalDate.of(2027, 1, 7)));
    }

    @Test
    void givenAnEurderOfDamagedUnits_whenReorderingAfterAPriceChange_thenReordersTheSameStateAtTheNewDiscountedPrice() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 0);
        itemUnitRepository.saveAll(chair.newUnits(ItemState.DAMAGED, 4));
        EurderDto original = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 2, ItemState.DAMAGED))));
        itemService.update(chair.getId(), new UpdateItemDto("Chair", "Wooden chair", new BigDecimal("60.00")));

        EurderDto reordered = eurderService.reorder(janeCredentialsId, original.id());

        assertThat(reordered.itemGroups())
                .extracting(ItemGroupDto::itemState, ItemGroupDto::itemPrice, ItemGroupDto::amount)
                .containsExactly(tuple(ItemState.DAMAGED, new BigDecimal("54.00"), 2));
        assertThat(availableUnits(chair)).isZero();
    }

    @Test
    void givenAnEurderOfAnotherCustomer_whenReordering_thenThrowsEurderNotFoundAndOrdersNothing() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 10);
        EurderDto zoes = eurderService.order(registerCustomer("zoe@adams.be"), new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 2))));

        Throwable thrown = catchThrowable(() -> eurderService.reorder(janeCredentialsId, zoes.id()));

        assertThat(thrown)
                .isInstanceOf(EurderNotFoundException.class)
                .hasMessage("No eurder found for id " + zoes.id());
        assertThat(eurderRepository.count()).isEqualTo(1);
        assertThat(availableUnits(chair)).isEqualTo(8);
    }

    @Test
    void givenUnitsInTwoStates_whenOrderingDamagedOnes_thenChargesTheDiscountAndLinksExactlyThoseUnitsToTheItemGroup() {
        Item chair = itemWithPristineUnits("Chair", "Wooden chair", "49.99", 3);
        itemUnitRepository.saveAll(chair.newUnits(ItemState.DAMAGED, 3));

        EurderDto eurder = eurderService.order(janeCredentialsId, new CreateEurderDto(List.of(new ItemGroupRequestDto(chair.getId(), 2, ItemState.DAMAGED))));

        assertThat(eurder.itemGroups()).singleElement().satisfies(itemGroup -> {
            assertThat(itemGroup.itemState()).isEqualTo(ItemState.DAMAGED);
            assertThat(itemGroup.itemPrice()).isEqualTo(new BigDecimal("44.99"));
            assertThat(itemGroup.subtotal()).isEqualTo(new BigDecimal("89.98"));
        });
        assertThat(itemUnitRepository.findAll())
                .filteredOn(unit -> !unit.isAvailable())
                .hasSize(2)
                .allSatisfy(unit -> assertThat(unit.getState()).isEqualTo(ItemState.DAMAGED));
        assertThat(itemUnitRepository.countAvailableUnits(List.of(chair.getId())))
                .extracting(AvailableUnits::state, AvailableUnits::count)
                .containsExactlyInAnyOrder(tuple(ItemState.PRISTINE, 3L), tuple(ItemState.DAMAGED, 1L));
    }

    private Item itemWithPristineUnits(String name, String description, String price, int units) {
        Item item = itemRepository.save(new Item(name, description, new BigDecimal(price)));
        itemUnitRepository.saveAll(item.newUnits(ItemState.PRISTINE, units));
        return item;
    }

    private long availableUnits(Item item) {
        return itemUnitRepository.countAvailableUnits(List.of(item.getId())).stream().mapToLong(AvailableUnits::count).sum();
    }

    private UUID registerCustomer(String email) {
        customerService.register(new CreateCustomerDto("Zoe", "Adams", email, "secret123",
                new AddressDto("Rue Haute", "1", "1000", "Bruxelles"), "0470123456"));
        return credentialsRepository.findByEmail(email).orElseThrow().getId();
    }
}
