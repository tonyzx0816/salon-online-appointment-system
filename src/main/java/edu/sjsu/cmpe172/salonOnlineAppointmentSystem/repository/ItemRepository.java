package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.model.Item;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ItemRepository {

    private final Map<Long, Item> store = new HashMap<>();
    private long nextId = 1L;

    public List<Item> findAll() {
        return new ArrayList<>(store.values());
    }

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(nextId++);
        }
        store.put(item.getId(), item);
        return item;
    }
}
