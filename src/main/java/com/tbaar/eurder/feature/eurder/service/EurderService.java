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
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Customer customer = findCustomer(credentialsId);
        List<Reservation> reservations = reserve(request.itemGroups());
        Eurder eurder = eurderRepository.save(new Eurder(customer, reservations.stream().map(Reservation::itemGroup).toList(), clock.instant()));
        reservations.forEach(Reservation::assignUnits);
        return eurderMapper.toDto(eurder);
    }

    private Customer findCustomer(UUID credentialsId) {
        return customerRepository.findByCredentialsId(credentialsId)
                .orElseThrow(() -> new IllegalStateException("No customer linked to credentials " + credentialsId));
    }

    private List<Reservation> reserve(List<ItemGroupRequestDto> requests) {
        LocalDate orderDate = LocalDate.now(clock);
        Map<UnitsOfAState, Deque<ItemUnit>> availableUnits = findAvailableUnits(requests);
        return requests.stream()
                .map(requested -> reserve(requested, availableUnits.get(UnitsOfAState.from(requested)), orderDate))
                .toList();
    }

    private Map<UnitsOfAState, Deque<ItemUnit>> findAvailableUnits(List<ItemGroupRequestDto> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(UnitsOfAState::from, Collectors.summingInt(ItemGroupRequestDto::amount)))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, requested -> findAvailableUnits(requested.getKey(), requested.getValue())));
    }

    private Deque<ItemUnit> findAvailableUnits(UnitsOfAState units, int amount) {
        return new ArrayDeque<>(itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(units.itemId(), units.state(), Limit.of(amount)));
    }

    private Reservation reserve(ItemGroupRequestDto requested, Deque<ItemUnit> available, LocalDate orderDate) {
        Item item = itemRepository.findLockedById(requested.itemId())
                .orElseThrow(() -> new ItemNotFoundException(requested.itemId()));
        ItemGroup itemGroup = ItemGroup.of(item, requested.state(), requested.amount(), available.size(), orderDate);
        return new Reservation(itemGroup, take(available, requested.amount()));
    }

    private static List<ItemUnit> take(Deque<ItemUnit> available, int amount) {
        return Stream.generate(available::poll)
                .limit(Math.min(amount, available.size()))
                .toList();
    }

    private record UnitsOfAState(UUID itemId, ItemState state) {

        static UnitsOfAState from(ItemGroupRequestDto requested) {
            return new UnitsOfAState(requested.itemId(), requested.state());
        }
    }

    private record Reservation(ItemGroup itemGroup, List<ItemUnit> units) {

        void assignUnits() {
            units.forEach(unit -> unit.assignTo(itemGroup.getId()));
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
                .map(itemGroup -> new ItemGroupRequestDto(itemGroup.getItemId(), itemGroup.getAmount(), itemGroup.getItemState()))
                .toList();
        return order(credentialsId, new CreateEurderDto(sameItems));
    }
}
