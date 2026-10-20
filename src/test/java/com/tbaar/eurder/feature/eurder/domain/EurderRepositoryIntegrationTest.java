package com.tbaar.eurder.feature.eurder.domain;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class EurderRepositoryIntegrationTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 12, 31);
    private static final Instant ORDERED_AT = Instant.parse("2026-12-31T09:00:00Z");

    @Autowired
    private EurderRepository eurderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void givenEurderWithItemGroups_whenSavingIt_thenTheItemGroupsAreStoredTooInTheirOrder() {
        Customer jane = customerRepository.save(aCustomer());
        Item table = itemRepository.save(new Item("Table", "Oak table", new BigDecimal("199.00"), 0));
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        Eurder saved = eurderRepository.saveAndFlush(new Eurder(jane, List.of(
                ItemGroup.of(table, 1, ORDER_DATE),
                ItemGroup.of(chair, 2, ORDER_DATE)), ORDERED_AT));
        entityManager.clear();

        Optional<Eurder> found = eurderRepository.findById(saved.getId());

        assertThat(found).hasValueSatisfying(eurder -> {
            assertThat(eurder.getCustomer().getId()).isEqualTo(jane.getId());
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
    void givenEurderWithTwoItemGroups_whenSavingIt_thenRunsOnlyThreeInsertsAndNoUpdate() {
        Customer jane = customerRepository.saveAndFlush(aCustomer());
        Item chair = itemRepository.saveAndFlush(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        Eurder eurder = new Eurder(jane, List.of(ItemGroup.of(chair, 1, ORDER_DATE), ItemGroup.of(chair, 2, ORDER_DATE)), ORDERED_AT);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        eurderRepository.saveAndFlush(eurder);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void givenEurdersOfTwoCustomers_whenFindingByCredentials_thenReturnsOnlyThatCustomersEurdersOldestFirst() {
        Customer jane = customerRepository.save(aCustomer("jane@doe.be"));
        Customer zoe = customerRepository.save(aCustomer("zoe@adams.be"));
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        Eurder janesLatest = eurderRepository.save(anEurder(jane, chair, 3, "2026-12-31T15:00:00Z"));
        eurderRepository.save(anEurder(zoe, chair, 1, "2026-12-31T12:00:00Z"));
        Eurder janesFirst = eurderRepository.saveAndFlush(anEurder(jane, chair, 2, "2026-12-30T09:00:00Z"));
        entityManager.clear();

        List<Eurder> eurders = eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(jane.getCredentials().getId());

        assertThat(eurders).extracting(Eurder::getId).containsExactly(janesFirst.getId(), janesLatest.getId());
    }

    @Test
    void givenThreeEurdersWithItemGroups_whenFindingByCredentialsAndReadingTheirItemGroups_thenRunsASingleQuery() {
        Customer jane = customerRepository.save(aCustomer("jane@doe.be"));
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10));
        eurderRepository.save(anEurder(jane, chair, 1, "2026-12-29T09:00:00Z"));
        eurderRepository.save(anEurder(jane, chair, 2, "2026-12-30T09:00:00Z"));
        eurderRepository.saveAndFlush(anEurder(jane, chair, 3, "2026-12-31T09:00:00Z"));
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<Eurder> eurders = eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(jane.getCredentials().getId());

        assertThat(eurders).flatExtracting(Eurder::getItemGroups).extracting(ItemGroup::getAmount).containsExactly(1, 2, 3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private static Eurder anEurder(Customer customer, Item item, int amount, String orderedAt) {
        return new Eurder(customer, List.of(ItemGroup.of(item, amount, ORDER_DATE)), Instant.parse(orderedAt));
    }

    private static Customer aCustomer() {
        return aCustomer("jane@doe.be");
    }

    private static Customer aCustomer(String email) {
        return new Customer(new Credentials(email, "hash", Role.CUSTOMER),
                "Jane", "Doe", new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
