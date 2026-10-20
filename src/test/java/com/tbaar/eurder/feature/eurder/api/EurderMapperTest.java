package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.customer.domain.Address;
import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.security.domain.Credentials;
import com.tbaar.eurder.feature.security.domain.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EurderMapperTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 12, 31);
    private static final Instant ORDERED_AT = Instant.parse("2026-12-31T09:00:00Z");

    private final EurderMapper eurderMapper = new EurderMapper();

    @Test
    void givenEurder_whenMappingToDto_thenExposesEachItemGroupWithItsSubtotalAndTheTotalPrice() {
        Eurder eurder = new Eurder(aCustomer(), List.of(
                ItemGroup.of(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10), 3, ORDER_DATE),
                ItemGroup.of(new Item("Table", "Oak table", new BigDecimal("199.00"), 0), 1, ORDER_DATE)), ORDERED_AT);

        EurderDto dto = eurderMapper.toDto(eurder);

        assertThat(dto).isEqualTo(new EurderDto(eurder.getId(), List.of(
                new ItemGroupDto(null, "Chair", new BigDecimal("49.99"), 3, new BigDecimal("149.97"), LocalDate.of(2027, 1, 1)),
                new ItemGroupDto(null, "Table", new BigDecimal("199.00"), 1, new BigDecimal("199.00"), LocalDate.of(2027, 1, 7))),
                new BigDecimal("348.97")));
    }

    @Test
    void givenTwoEurders_whenMappingToReport_thenListsEachEurderWithItsTotalAndTheTotalOfAllEurders() {
        Eurder first = new Eurder(aCustomer(), List.of(
                ItemGroup.of(new Item("Chair", "Wooden chair", new BigDecimal("49.99"), 10), 3, ORDER_DATE),
                ItemGroup.of(new Item("Lamp", "Desk lamp", new BigDecimal("0.10"), 10), 3, ORDER_DATE)), ORDERED_AT);
        Eurder second = new Eurder(aCustomer(), List.of(
                ItemGroup.of(new Item("Table", "Oak table", new BigDecimal("199.00"), 0), 1, ORDER_DATE)), ORDERED_AT);

        ReportDto report = eurderMapper.toReportDto(List.of(first, second));

        assertThat(report).isEqualTo(new ReportDto(List.of(
                new EurderReportDto(first.getId(), List.of(
                        new ItemGroupReportDto("Chair", 3, new BigDecimal("149.97")),
                        new ItemGroupReportDto("Lamp", 3, new BigDecimal("0.30"))),
                        new BigDecimal("150.27")),
                new EurderReportDto(second.getId(), List.of(
                        new ItemGroupReportDto("Table", 1, new BigDecimal("199.00"))),
                        new BigDecimal("199.00"))),
                new BigDecimal("349.27")));
    }

    @Test
    void givenNoEurder_whenMappingToReport_thenReturnsAnEmptyListAndATotalOfZeroWithTwoDecimals() {
        List<Eurder> noEurders = List.of();

        ReportDto report = eurderMapper.toReportDto(noEurders);

        assertThat(report.eurders()).isEmpty();
        assertThat(report.totalPrice()).isEqualTo(new BigDecimal("0.00"));
    }

    private static Customer aCustomer() {
        return new Customer(new Credentials("jane@doe.be", "hash", Role.CUSTOMER), "Jane", "Doe",
                new Address("Rue Haute", "1", "1000", "Bruxelles"), "0470123456");
    }
}
