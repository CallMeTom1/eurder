package com.tbaar.eurder.feature.item.service;

import com.tbaar.eurder.feature.item.api.ItemMapper;
import com.tbaar.eurder.feature.item.api.dto.ItemDto;
import com.tbaar.eurder.feature.item.api.dto.ItemRequestDto;
import com.tbaar.eurder.feature.item.domain.Item;
import com.tbaar.eurder.feature.item.domain.ItemRepository;
import com.tbaar.eurder.feature.item.exception.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public ItemDto create(ItemRequestDto request) {
        Item item = itemRepository.save(itemMapper.toEntity(request));
        return itemMapper.toDto(item);
    }

    public ItemDto update(UUID id, ItemRequestDto request) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        item.update(request.name(), request.description(), request.price(), request.stock());
        return itemMapper.toDto(item);
    }
}
