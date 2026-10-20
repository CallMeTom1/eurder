package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import org.springframework.stereotype.Component;

@Component
public class EurderMapper {

    public EurderDto toDto(Eurder eurder) {
        return new EurderDto(eurder.getId(),
                eurder.getItemGroups().stream().map(this::toDto).toList(),
                eurder.totalPrice());
    }

    private ItemGroupDto toDto(ItemGroup itemGroup) {
        return new ItemGroupDto(itemGroup.getItemId(),
                itemGroup.getItemName(),
                itemGroup.getItemPrice(),
                itemGroup.getAmount(),
                itemGroup.subtotal(),
                itemGroup.getShippingDate());
    }
}
