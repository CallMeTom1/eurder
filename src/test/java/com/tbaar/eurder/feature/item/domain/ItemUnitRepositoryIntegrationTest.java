package com.tbaar.eurder.feature.item.domain;

import com.tbaar.eurder.common.config.MongoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataMongoTest
@Import(MongoConfig.class)
class ItemUnitRepositoryIntegrationTest {

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void givenAvailableAndAssignedUnitsOfSeveralStates_whenFindingAvailableUnitsOfAState_thenReturnsOnlyThoseUpToTheLimit() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99")));
        itemUnitRepository.insert(chair.newUnits(ItemState.PRISTINE, 3));
        itemUnitRepository.insert(chair.newUnits(ItemState.DAMAGED, 2));
        ItemUnit sold = new ItemUnit(chair, ItemState.PRISTINE);
        sold.assignTo(UUID.randomUUID());
        itemUnitRepository.save(sold);

        List<ItemUnit> twoOfThem = itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(chair.getId(), ItemState.PRISTINE, Limit.of(2));
        List<ItemUnit> allOfThem = itemUnitRepository.findByItemIdAndStateAndItemGroupIdIsNull(chair.getId(), ItemState.PRISTINE, Limit.of(10));

        assertThat(twoOfThem).hasSize(2).allSatisfy(unit -> assertThat(unit.isAvailable()).isTrue());
        assertThat(allOfThem).hasSize(3).extracting(ItemUnit::getState).containsOnly(ItemState.PRISTINE);
    }

    @Test
    void givenUnitsOfTwoItems_whenCountingTheAvailableUnits_thenCountsThemPerItemAndState() {
        Item chair = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99")));
        Item lamp = itemRepository.save(new Item("Lamp", "Desk lamp", new BigDecimal("9.99")));
        itemUnitRepository.insert(chair.newUnits(ItemState.PRISTINE, 2));
        itemUnitRepository.insert(chair.newUnits(ItemState.DAMAGED, 1));
        itemUnitRepository.insert(lamp.newUnits(ItemState.SLIGHTLY_DAMAGED, 4));
        ItemUnit sold = new ItemUnit(lamp, ItemState.SLIGHTLY_DAMAGED);
        sold.assignTo(UUID.randomUUID());
        itemUnitRepository.save(sold);

        List<AvailableUnits> counts = itemUnitRepository.countAvailableUnits(List.of(chair.getId(), lamp.getId()));

        assertThat(counts)
                .extracting(AvailableUnits::itemId, AvailableUnits::state, AvailableUnits::count)
                .containsExactlyInAnyOrder(
                        tuple(chair.getId(), ItemState.PRISTINE, 2L),
                        tuple(chair.getId(), ItemState.DAMAGED, 1L),
                        tuple(lamp.getId(), ItemState.SLIGHTLY_DAMAGED, 4L));
    }
}
