package ru.andreevcode.logicore.corelogistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.andreevcode.logicore.corelogistics.data.RequestHubDto;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.data.TransportHubEntity;
import ru.andreevcode.logicore.corelogistics.exception.HubNotFoundException;
import ru.andreevcode.logicore.corelogistics.exception.NotEnoughCapacityException;
import ru.andreevcode.logicore.corelogistics.repo.TransportHubRepository;

import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableResilientMethods
public class TransportHubService {
    private static final String NO_HUB_EXCEPTION = "No transport hub found for id=";
    private static final Function<TransportHubEntity, ResponseHubDto> TO_DTO_MAPPER =
            entity -> new ResponseHubDto(
                    entity.getId(),
                    entity.getName(),
                    entity.getCapacity(),
                    entity.getCode());

    private static final Function<RequestHubDto, TransportHubEntity> TO_ENTITY_MAPPER =
            dto -> new TransportHubEntity(
                    null,
                    dto.name(),
                    dto.capacity(),
                    dto.code(),
                    null
            );

    private final TransportHubRepository repository;

    public List<ResponseHubDto> findAll() {
        return repository.findAll().stream()
                .map(TO_DTO_MAPPER)
                .toList();
    }

    @Transactional
    public ResponseHubDto insert(RequestHubDto requestHubDto) {
        TransportHubEntity entity = repository.insert(TO_ENTITY_MAPPER.apply(requestHubDto));
        return TO_DTO_MAPPER.apply(entity);
    }

    public ResponseHubDto findById(Long id) {
        return repository.findById(id)
                .map(TO_DTO_MAPPER)
                .orElseThrow(() -> new HubNotFoundException(NO_HUB_EXCEPTION + id));
    }

    @Retryable(
            value = OptimisticLockingFailureException.class,
            maxRetries = 15,
            delay = 200,
            multiplier = 2.0,
            jitter = 50
    )
    @Transactional
    public ResponseHubDto updateCapacity(long id, int amount) {
        var currentHub = repository.findById(id)
                .orElseThrow(() -> new HubNotFoundException(NO_HUB_EXCEPTION + id));

        if (currentHub.getCapacity() + amount < 0) {
            var msg = String.format("Not enough capacity(%d) for request(%d) at hub id=%d, version=%d"
                    , currentHub.getCapacity(), amount, id, currentHub.getVersion());
            log.error(msg);
            throw new NotEnoughCapacityException(msg);
        }
        long newVersion = currentHub.getVersion() + 1;
        int newCapacity = currentHub.getCapacity() + amount;
        int rowsUpdated = repository.updateCapacity(id, newCapacity, currentHub.getVersion(), newVersion);
        if (rowsUpdated == 0) {
            throw new OptimisticLockingFailureException(
                    String.format("%d hub's capacity was modified by another transaction", id));
        }
        currentHub.setVersion(newVersion);
        currentHub.setCapacity(newCapacity);
        return TO_DTO_MAPPER.apply(currentHub);
    }
}
