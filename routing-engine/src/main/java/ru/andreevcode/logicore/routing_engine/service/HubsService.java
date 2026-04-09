package ru.andreevcode.logicore.routing_engine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubCapacityDepletedEvent;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubsEntity;
import ru.andreevcode.logicore.routing_engine.kafka.exception.BadFormatKafkaConsumerException;
import ru.andreevcode.logicore.routing_engine.repo.HubsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HubsService {
    private final HubsRepository hubsRepository;

    @Transactional
    public int updateCapacity(HubCapacityDepletedEvent event) {
        if (event.eventId() == null || event.hubId() == null || event.hubId() < 0 || event.code() == null) {
            String msg = String.format("Invalid event payload: missing mandatory fields: eventId=%s, hubId=%s, code=%s",
                    event.eventId(), event.hubId(), event.code());
            throw new BadFormatKafkaConsumerException(msg);
        }
        return hubsRepository.upsertCapacity(
                new HubsEntity(event.hubId(), event.code(), event.remainingCapacity(), event.createdAt()));
    }

    @Transactional(readOnly = true)
    public List<HubsEntity> findAll() {
        return hubsRepository.findAll();
    }
}
