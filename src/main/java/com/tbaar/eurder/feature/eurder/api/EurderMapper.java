package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.customer.api.AddressMapper;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingItemGroupDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.eurder.domain.ItemGroupToShip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EurderMapper {

    private final AddressMapper addressMapper;

    public EurderMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public EurderDto toDto(Eurder eurder) {
        return new EurderDto(eurder.getId(),
                eurder.getItemGroups().stream().map(this::toDto).toList(),
                eurder.totalPrice());
    }

    private ItemGroupDto toDto(ItemGroup itemGroup) {
        return new ItemGroupDto(itemGroup.getItemId(),
                itemGroup.getItemName(),
                itemGroup.getItemState(),
                itemGroup.getItemPrice(),
                itemGroup.getAmount(),
                itemGroup.subtotal(),
                itemGroup.getShippingDate());
    }

    public ReportDto toReportDto(List<Eurder> eurders) {
        BigDecimal totalPrice = eurders.stream()
                .map(Eurder::totalPrice)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new ReportDto(eurders.stream().map(this::toReportDto).toList(), totalPrice);
    }

    private EurderReportDto toReportDto(Eurder eurder) {
        return new EurderReportDto(eurder.getId(),
                eurder.getItemGroups().stream().map(this::toReportDto).toList(),
                eurder.totalPrice());
    }

    private ItemGroupReportDto toReportDto(ItemGroup itemGroup) {
        return new ItemGroupReportDto(itemGroup.getItemName(), itemGroup.getItemState(), itemGroup.getAmount(), itemGroup.subtotal());
    }

    public List<ShippingEurderDto> toShippingDtos(List<ItemGroupToShip> itemGroups) {
        return itemGroups.stream()
                .collect(Collectors.groupingBy(ItemGroupToShip::eurderId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toShippingDto)
                .toList();
    }

    private ShippingEurderDto toShippingDto(List<ItemGroupToShip> itemGroupsOfOneEurder) {
        ItemGroupToShip first = itemGroupsOfOneEurder.get(0);
        return new ShippingEurderDto(first.eurderId(),
                first.customerFirstName() + " " + first.customerLastName(),
                addressMapper.toDto(first.address()),
                itemGroupsOfOneEurder.stream()
                        .map(itemGroup -> new ShippingItemGroupDto(itemGroup.itemName(), itemGroup.itemState(), itemGroup.amount()))
                        .toList());
    }
}
