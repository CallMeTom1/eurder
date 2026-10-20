package com.tbaar.eurder.feature.eurder.api;

import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
        return new ItemGroupReportDto(itemGroup.getItemName(), itemGroup.getAmount(), itemGroup.subtotal());
    }
}
