package ru.andreevcode.logicore.corelogistics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.andreevcode.logicore.corelogistics.data.RequestHubDto;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.data.TransportHubEntity;
import ru.andreevcode.logicore.corelogistics.exception.HubNotFoundException;
import ru.andreevcode.logicore.corelogistics.repo.TransportHubRepository;

import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TransportHubService {
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
                    dto.code());

    private final TransportHubRepository repository;

    public List<ResponseHubDto> findAll() {
        return repository.findAll().stream()
                .map(TO_DTO_MAPPER)
                .toList();
    }

    @Transactional
    public ResponseHubDto save(RequestHubDto requestHubDto) {
        TransportHubEntity entity = repository.save(TO_ENTITY_MAPPER.apply(requestHubDto));
        return TO_DTO_MAPPER.apply(entity);
    }

    public ResponseHubDto findById(Long id) {
        return repository.findById(id)
                .map(TO_DTO_MAPPER)
                .orElseThrow(() -> new HubNotFoundException("No transport hub found for id=" + id));
    }
}
