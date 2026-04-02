package ru.andreevcode.logicore.corelogistics.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OutboxEventsMarker {

    public List<OutboxEntity> markObsoleteSkipped(List<OutboxEntity> groupEvents) {
        if (groupEvents.isEmpty()) {
            return groupEvents;
        }

        OutboxEventType type = groupEvents.get(0).getEventType();
        return switch (type) {
            case HUB_CAPACITY_DEPLETED -> markObsoleteSkipped(groupEvents, OutboxEntity::getAggregationId);
            default -> groupEvents;
        };
    }

    private List<OutboxEntity> markObsoleteSkipped(
            List<OutboxEntity> groupEvents,
            Function<OutboxEntity, String> keyGrouper) {
        Map<?, OutboxEntity> latestEvents = groupEvents.stream()
                .collect(Collectors.toMap(
                        keyGrouper::apply,
                        entity -> entity,
                        (existing, replacement) ->
                                existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
                ));

        groupEvents.forEach(entity -> {
            var latest = latestEvents.get(keyGrouper.apply(entity));
            if (!entity.getId().equals(latest.getId())){
                entity.setEventStatus(OutboxEventStatus.SKIPPED);
                entity.setUpdatedAt(Instant.now());
            }
        });

        return groupEvents;
    }
}
