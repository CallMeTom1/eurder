package com.tbaar.eurder.feature.item.domain;

import com.tbaar.eurder.common.config.MongoConfig;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@DataMongoTest
@Import(MongoConfig.class)
class ItemRepositoryIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUnitRepository itemUnitRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void givenSavedItem_whenReadingTheRawDocument_thenThePriceIsADecimal128() {
        Item saved = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.90")));

        Document document = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(saved.getId())), Document.class, "items");

        assertThat(document.get("price")).isEqualTo(new Decimal128(new BigDecimal("49.90")));
    }

    @Test
    void givenAnItemChangedSinceItWasRead_whenSavingTheStaleCopy_thenFailsAndKeepsTheOtherChange() {
        Item saved = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99")));
        Item stale = itemRepository.findById(saved.getId()).orElseThrow();
        Item fresh = itemRepository.findById(saved.getId()).orElseThrow();
        fresh.update("Chair", "Wooden chair", new BigDecimal("29.99"));
        itemRepository.save(fresh);
        stale.update("Chair", "Wooden chair", new BigDecimal("39.99"));

        Throwable thrown = catchThrowable(() -> itemRepository.save(stale));

        assertThat(thrown).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(itemRepository.findById(saved.getId())).hasValueSatisfying(item ->
                assertThat(item.getPrice()).isEqualTo(new BigDecimal("29.99")));
    }

    @Test
    void givenSavedItem_whenReadingItBackFromTheDatabase_thenEveryFieldIsStored() {
        Item saved = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99")));

        Optional<Item> found = itemRepository.findById(saved.getId());

        assertThat(found).hasValueSatisfying(item -> {
            assertThat(item.getName()).isEqualTo("Chair");
            assertThat(item.getDescription()).isEqualTo("Wooden chair");
            assertThat(item.getPrice()).isEqualTo(new BigDecimal("49.99"));
        });
    }

    @Test
    void givenItemWithTheNameOfAnExistingItem_whenSaving_thenBothItemsAreStored() {
        Item wooden = itemRepository.save(new Item("Chair", "Wooden chair", new BigDecimal("49.99")));

        Item plastic = itemRepository.save(new Item("Chair", "Plastic chair", new BigDecimal("19.99")));

        assertThat(itemRepository.findAllById(List.of(wooden.getId(), plastic.getId()))).hasSize(2);
    }

    @Test
    void givenItemsWithAvailableAndSoldUnits_whenFindingTheirStock_thenCountsOnlyAvailableUnitsAndSortsByThatCountThenName() {
        Set<UUID> ownItems = saveItems();

        List<ItemStock> stock = itemRepository.findStockBetween(0, Integer.MAX_VALUE);

        assertThat(stock)
                .filteredOn(itemStock -> ownItems.contains(itemStock.item().getId()))
                .extracting(itemStock -> itemStock.item().getName(), ItemStock::availableUnits)
                .containsExactly(tuple("Lamp", 0L), tuple("Bed", 4L), tuple("Desk", 4L), tuple("Chair", 7L), tuple("Table", 12L));
    }

    @Test
    void givenItemsWithAvailableUnits_whenFindingTheStockWithinARange_thenReturnsOnlyThatRangeSortedByCountThenName() {
        Set<UUID> ownItems = saveItems();

        List<ItemStock> stock = itemRepository.findStockBetween(0, 4);

        assertThat(stock)
                .filteredOn(itemStock -> ownItems.contains(itemStock.item().getId()))
                .extracting(itemStock -> itemStock.item().getName())
                .containsExactly("Lamp", "Bed", "Desk");
    }

    private Set<UUID> saveItems() {
        Item table = saveItem("Table", ItemState.PRISTINE, 12);
        Item desk = saveItem("Desk", ItemState.PRISTINE, 3);
        itemUnitRepository.insert(desk.newUnits(ItemState.DAMAGED, 1));
        Item lamp = saveItem("Lamp", ItemState.PRISTINE, 0);
        List<ItemUnit> soldLamps = lamp.newUnits(ItemState.PRISTINE, 2);
        soldLamps.forEach(unit -> unit.assignTo(UUID.randomUUID()));
        itemUnitRepository.insert(soldLamps);
        Item chair = saveItem("Chair", ItemState.PRISTINE, 7);
        Item bed = saveItem("Bed", ItemState.SLIGHTLY_DAMAGED, 4);
        return Set.of(table.getId(), desk.getId(), lamp.getId(), chair.getId(), bed.getId());
    }

    private Item saveItem(String name, ItemState state, int units) {
        Item item = itemRepository.save(new Item(name, name + " description", new BigDecimal("49.99")));
        itemUnitRepository.insert(item.newUnits(state, units));
        return item;
    }
}
