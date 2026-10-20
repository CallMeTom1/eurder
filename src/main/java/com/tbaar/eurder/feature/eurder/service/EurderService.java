package com.tbaar.eurder.feature.eurder.service;

import com.tbaar.eurder.feature.customer.domain.Customer;
import com.tbaar.eurder.feature.customer.domain.CustomerRepository;
import com.tbaar.eurder.feature.eurder.api.EurderMapper;
import com.tbaar.eurder.feature.eurder.api.dto.CreateEurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.EurderDto;
import com.tbaar.eurder.feature.eurder.api.dto.ItemGroupRequestDto;
import com.tbaar.eurder.feature.eurder.api.dto.ReportDto;
import com.tbaar.eurder.feature.eurder.api.dto.ShippingEurderDto;
import com.tbaar.eurder.feature.eurder.domain.Eurder;
import com.tbaar.eurder.feature.eurder.domain.EurderRepository;
import com.tbaar.eurder.feature.eurder.domain.ItemGroup;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EurderService {

    private final EurderRepository eurderRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final EurderMapper eurderMapper;
    private final Clock clock;

    public EurderService(EurderRepository eurderRepository, CustomerRepository customerRepository, ItemRepository itemRepository,
                         EurderMapper eurderMapper, Clock clock) {
        this.eurderRepository = eurderRepository;
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
        this.eurderMapper = eurderMapper;
        this.clock = clock;
    }

    public EurderDto order(UUID credentialsId, CreateEurderDto request) {
        Customer customer = customerRepository.findByCredentialsId(credentialsId)
                .orElseThrow(() -> new IllegalStateException("No customer linked to credentials " + credentialsId));
        LocalDate orderDate = LocalDate.now(clock);
        List<ItemGroup> itemGroups = new ArrayList<>();
        for (ItemGroupRequestDto requested : request.itemGroups()) {
            Item item = itemRepository.findById(requested.itemId())
                    .orElseThrow(() -> new ItemNotFoundException(requested.itemId()));
            itemGroups.add(ItemGroup.of(item, requested.amount(), orderDate));
            item.removeFromStock(requested.amount());
        }
        Eurder eurder = eurderRepository.save(new Eurder(customer, itemGroups, clock.instant()));
        return eurderMapper.toDto(eurder);
    }

    @Transactional(readOnly = true)
    public ReportDto findReport(UUID credentialsId) {
        return eurderMapper.toReportDto(eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(credentialsId));
    }

    @Transactional(readOnly = true)
    public List<ShippingEurderDto> findShippingToday() {
        return eurderMapper.toShippingDtos(eurderRepository.findItemGroupsShippingOn(LocalDate.now(clock)));
    }
}
