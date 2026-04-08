package ru.andreevcode.logicore.routing_engine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubsEntity;
import ru.andreevcode.logicore.routing_engine.repo.HubsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubsService {
    private final HubsRepository hubsRepository;

    public int updateCapacity(HubCapacityDepletedEvent event) {
        if (event.eventId() == null || event.hubId() == null || event.code() == null) {
            return 0;
        }
        return hubsRepository.upsertCapacity(
                new HubsEntity(event.hubId(), event.code(), event.remainingCapacity(), event.createdAt()));
    }

    public List<HubsEntity> findAll() {
        return hubsRepository.findAll();
    }
}
