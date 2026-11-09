package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.common.UniqueEmail;
import com.tbaar.eurder.common.config.MongoConfig;
import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataMongoTest
@Import(MongoConfig.class)
class EurderRepositoryIntegrationTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 12, 31);
    private static final Instant ORDERED_AT = Instant.parse("2026-12-31T09:00:00Z");

    @Autowired
    private EurderRepository eurderRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void givenEurderWithItemGroups_whenSavingIt_thenTheItemGroupsAreStoredTooInTheirOrder() {
        Customer jane = aCustomer();
        Item table = new Item("Table", "Oak table", new BigDecimal("199.00"));
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        Eurder saved = eurderRepository.save(new Eurder(jane, List.of(
                ItemGroup.of(table, ItemState.PRISTINE, 1, 0, ORDER_DATE),
                ItemGroup.of(chair, ItemState.PRISTINE, 2, 10, ORDER_DATE)), ORDERED_AT));

        Optional<Eurder> found = eurderRepository.findById(saved.getId());

        assertThat(found).hasValueSatisfying(eurder -> {
            assertThat(eurder.getCustomer().id()).isEqualTo(jane.getId());
            assertThat(eurder.getCustomer().address().getCity()).isEqualTo("Bruxelles");
            assertThat(eurder.getOrderedAt()).isEqualTo(ORDERED_AT);
            assertThat(eurder.getItemGroups())
                    .extracting(ItemGroup::getItemId, ItemGroup::getItemName, ItemGroup::getItemPrice, ItemGroup::getAmount, ItemGroup::getShippingDate)
                    .containsExactly(
                            tuple(table.getId(), "Table", new BigDecimal("199.00"), 1, LocalDate.of(2027, 1, 7)),
                            tuple(chair.getId(), "Chair", new BigDecimal("49.99"), 2, LocalDate.of(2027, 1, 1)));
            assertThat(eurder.totalPrice()).isEqualTo(new BigDecimal("298.98"));
        });
    }

    @Test
    void givenSavedEurder_whenReadingTheRawDocument_thenTheShippingDateIsMidnightUtcWhateverTheZoneOfTheMachine() {
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        Eurder saved = eurderRepository.save(anEurder(aCustomer(), chair, 2, "2026-12-31T09:00:00Z"));

        Document document = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(saved.getId())), Document.class, "eurders");

        Document itemGroup = document.getList("itemGroups", Document.class).get(0);
        assertThat(itemGroup.get("shippingDate")).isEqualTo(Date.from(Instant.parse("2027-01-01T00:00:00Z")));
    }

    @Test
    void givenEurdersOfTwoCustomers_whenFindingByCredentials_thenReturnsOnlyThatCustomersEurdersOldestFirst() {
        Customer jane = aCustomer(UniqueEmail.of("jane"));
        Customer zoe = aCustomer(UniqueEmail.of("zoe"));
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        Eurder janesLatest = eurderRepository.save(anEurder(jane, chair, 3, "2026-12-31T15:00:00Z"));
        eurderRepository.save(anEurder(zoe, chair, 1, "2026-12-31T12:00:00Z"));
        Eurder janesFirst = eurderRepository.save(anEurder(jane, chair, 2, "2026-12-30T09:00:00Z"));

        List<Eurder> eurders = eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(jane.getCredentialsId());

        assertThat(eurders).extracting(Eurder::getId).containsExactly(janesFirst.getId(), janesLatest.getId());
    }

    @Test
    void givenItemGroupsShippingOnDifferentDays_whenFindingThoseShippingOnADate_thenReturnsOnlyThatDayOldestEurderFirst() {
        Customer jane = aCustomer(UniqueEmail.of("jane"));
        Customer zoe = new Customer(new Credentials(UniqueEmail.of("zoe"), "hash", Role.CUSTOMER),
                "Zoe", "Adams", new Address("Rue Neuve", "8", "1000", "Bruxelles"), "0470654321");
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        Item table = new Item("Table", "Oak table", new BigDecimal("199.00"));
        LocalDate newYearsEve = LocalDate.of(2026, 12, 31);
        Eurder zoes = eurderRepository.save(new Eurder(zoe, List.of(ItemGroup.of(chair, ItemState.PRISTINE, 2, 10, newYearsEve)), Instant.parse("2026-12-31T10:00:00Z")));
        Eurder janes = eurderRepository.save(new Eurder(jane, List.of(
                ItemGroup.of(chair, ItemState.PRISTINE, 3, 10, newYearsEve), ItemGroup.of(table, ItemState.PRISTINE, 1, 0, newYearsEve)), Instant.parse("2026-12-31T09:00:00Z")));
        Eurder yesterdays = eurderRepository.save(new Eurder(jane, List.of(ItemGroup.of(chair, ItemState.PRISTINE, 4, 10, newYearsEve.minusDays(1))), Instant.parse("2026-12-30T09:00:00Z")));
        Eurder tomorrows = eurderRepository.save(new Eurder(jane, List.of(ItemGroup.of(chair, ItemState.PRISTINE, 5, 10, newYearsEve.plusDays(1))), Instant.parse("2027-01-01T09:00:00Z")));
        Set<UUID> ownEurders = Set.of(janes.getId(), zoes.getId(), yesterdays.getId(), tomorrows.getId());

        List<ItemGroupToShip> toShip = eurderRepository.findItemGroupsShippingOn(LocalDate.of(2027, 1, 1));

        List<ItemGroupToShip> ownToShip = toShip.stream().filter(itemGroup -> ownEurders.contains(itemGroup.eurderId())).toList();
        assertThat(ownToShip)
                .extracting(ItemGroupToShip::eurderId, ItemGroupToShip::customerFirstName, ItemGroupToShip::itemName, ItemGroupToShip::itemState, ItemGroupToShip::amount)
                .containsExactly(tuple(janes.getId(), "Jane", "Chair", ItemState.PRISTINE, 3), tuple(zoes.getId(), "Zoe", "Chair", ItemState.PRISTINE, 2));
        assertThat(ownToShip.get(1).address().getStreet()).isEqualTo("Rue Neuve");
    }

    @Test
    void givenEurdersOfTwoCustomers_whenFindingOneByIdAndCredentials_thenFindsOnlyAnEurderOfThatCustomer() {
        Customer jane = aCustomer(UniqueEmail.of("jane"));
        Customer zoe = aCustomer(UniqueEmail.of("zoe"));
        Item chair = new Item("Chair", "Wooden chair", new BigDecimal("49.99"));
        Eurder janes = eurderRepository.save(anEurder(jane, chair, 3, "2026-12-31T09:00:00Z"));
        Eurder zoes = eurderRepository.save(anEurder(zoe, chair, 1, "2026-12-31T10:00:00Z"));

        Optional<Eurder> own = eurderRepository.findByIdAndCustomerCredentialsId(janes.getId(), jane.getCredentialsId());
        Optional<Eurder> someoneElses = eurderRepository.findByIdAndCustomerCredentialsId(zoes.getId(), jane.getCredentialsId());

        assertThat(own).hasValueSatisfying(eurder ->
                assertThat(eurder.getItemGroups()).extracting(ItemGroup::getAmount).containsExactly(3));
        assertThat(someoneElses).isEmpty();
    }

    private static Eurder anEurder(Customer customer, Item item, int amount, String orderedAt) {
        return new Eurder(customer, List.of(ItemGroup.of(item, ItemState.PRISTINE, amount, 10, ORDER_DATE)), Instant.parse(orderedAt));
    }

    private static Customer aCustomer() {
        return aCustomer(UniqueEmail.of("jane"));
    }

    private static Customer aCustomer(String email) {
        return new Customer(new Credentials(email, "hash", Role.CUSTOMER),
                "Jane", "Doe", new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
