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
import com.tbaar.eurder.feature.eurder.exception.EurderNotFoundException;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.domain.ItemState;
import com.tbaar.eurder.feature.item.domain.ItemUnit;
import com.tbaar.eurder.feature.item.domain.ItemUnitRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EurderService {

    private final EurderRepository eurderRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final ItemUnitRepository itemUnitRepository;
    private final EurderMapper eurderMapper;
    private final Clock clock;

    public EurderService(EurderRepository eurderRepository, CustomerRepository customerRepository, ItemRepository itemRepository,
                         ItemUnitRepository itemUnitRepository, EurderMapper eurderMapper, Clock clock) {
        this.eurderRepository = eurderRepository;
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
        this.itemUnitRepository = itemUnitRepository;
        this.eurderMapper = eurderMapper;
        this.clock = clock;
    }

    public EurderDto order(UUID credentialsId, CreateEurderDto request) {
        Customer customer = customerRepository.findByCredentialsId(credentialsId)
                .orElseThrow(() -> new IllegalStateException("No customer linked to credentials " + credentialsId));
        LocalDate orderDate = LocalDate.now(clock);
        Map<UnitsOfAState, Integer> requestedAmounts = request.itemGroups().stream()
                .collect(Collectors.groupingBy(UnitsOfAState::of, Collectors.summingInt(ItemGroupRequestDto::amount)));
        Map<UnitsOfAState, Deque<ItemUnit>> availableUnits = new HashMap<>();
        List<ItemGroup> itemGroups = new ArrayList<>();
        List<List<ItemUnit>> unitsOfEachItemGroup = new ArrayList<>();
        for (ItemGroupRequestDto requested : request.itemGroups()) {
            Item item = itemRepository.findLockedById(requested.itemId())
                    .orElseThrow(() -> new ItemNotFoundException(requested.itemId()));
            Deque<ItemUnit> available = availableUnits.computeIfAbsent(UnitsOfAState.of(requested), units -> new ArrayDeque<>(
                    itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(units.itemId(), units.state(), Limit.of(requestedAmounts.get(units)))));
            itemGroups.add(ItemGroup.of(item, requested.state(), requested.amount(), available.size(), orderDate));
            unitsOfEachItemGroup.add(take(available, requested.amount()));
        }
        Eurder eurder = eurderRepository.save(new Eurder(customer, itemGroups, clock.instant()));
        for (int index = 0; index < itemGroups.size(); index++) {
            UUID itemGroupId = itemGroups.get(index).getId();
            unitsOfEachItemGroup.get(index).forEach(unit -> unit.assignTo(itemGroupId));
        }
        return eurderMapper.toDto(eurder);
    }

    private static List<ItemUnit> take(Deque<ItemUnit> available, int amount) {
        List<ItemUnit> taken = new ArrayList<>();
        while (taken.size() < amount && !available.isEmpty()) {
            taken.add(available.poll());
        }
        return taken;
    }

    private record UnitsOfAState(UUID itemId, ItemState state) {

        static UnitsOfAState of(ItemGroupRequestDto requested) {
            return new UnitsOfAState(requested.itemId(), requested.state());
        }
    }

    @Transactional(readOnly = true)
    public ReportDto findReport(UUID credentialsId) {
        return eurderMapper.toReportDto(eurderRepository.findAllByCustomerCredentialsIdOrderByOrderedAtAscIdAsc(credentialsId));
    }

    @Transactional(readOnly = true)
    public List<ShippingEurderDto> findShippingToday() {
        return eurderMapper.toShippingDtos(eurderRepository.findItemGroupsShippingOn(LocalDate.now(clock)));
    }

    public EurderDto reorder(UUID credentialsId, UUID eurderId) {
        Eurder existing = eurderRepository.findByIdAndCustomerCredentialsId(eurderId, credentialsId)
                .orElseThrow(() -> new EurderNotFoundException(eurderId));
        List<ItemGroupRequestDto> sameItems = existing.getItemGroups().stream()
                .map(itemGroup -> new ItemGroupRequestDto(itemGroup.getItemId(), itemGroup.getAmount()))
                .toList();
        return order(credentialsId, new CreateEurderDto(sameItems));
    }
}
