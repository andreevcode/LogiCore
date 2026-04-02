package ru.andreevcode.logicore.corelogistics.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.andreevcode.logicore.corelogistics.data.RequestHubDto;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.data.TransportHubEntity;
import ru.andreevcode.logicore.corelogistics.exception.HubNotFoundException;
import ru.andreevcode.logicore.corelogistics.exception.NotEnoughCapacityException;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;
import ru.andreevcode.logicore.corelogistics.repo.OutboxRepository;
import ru.andreevcode.logicore.corelogistics.repo.TransportHubRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.andreevcode.logicore.corelogistics.service.TransportHubService.MIN_CAPACITY_ALARM;

@ExtendWith(MockitoExtension.class)
class TransportHubServiceTest {

    TransportHubService transportHubService;

    @Mock
    TransportHubRepository transportHubRepository;

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    ObjectMapper objectMapper;

    @BeforeEach
    void init() {
        transportHubService = new TransportHubService(transportHubRepository, outboxRepository, objectMapper);
    }

    @Test
    void testInsertConversions() {
        var requestHubDto = new RequestHubDto("test-hub-1", 2, "hub-1");
        Mockito.when(transportHubRepository.insert(any()))
                .thenReturn(new TransportHubEntity(101L, "test-hub-1", 2, "hub-1", 0L));

        Assertions.assertEquals(new ResponseHubDto(101L, "test-hub-1", 2, "hub-1"),
                transportHubService.insert(requestHubDto)
        );
    }

    @Test
    void testFindByIdConversions() {
        final long id = 101L;
        Mockito.when(transportHubRepository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", 2, "hub-1", 0L)));

        assertThat(transportHubService.findById(id)).isEqualTo(new ResponseHubDto(101L, "test-hub-1", 2, "hub-1"));
    }

    @Test
    void shouldFindByIdThrowHubNotFoundException() {
        final long id = 101L;
        Mockito.when(transportHubRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transportHubService.findById(id))
                .isInstanceOf(HubNotFoundException.class)
                .hasMessage("No transport hub found for id=%d", id);
    }

    @Test
    void testUpdateCapacityWithOutbox() {
        final long id = 101L;
        final int startCapacity = 20;
        final int alarmCapacityChange = MIN_CAPACITY_ALARM - startCapacity;
        final int newCapacity = startCapacity + alarmCapacityChange;

        Mockito.when(transportHubRepository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", startCapacity, "hub-1", 0L)));

        Mockito.when(transportHubRepository.updateCapacity(id, newCapacity, 0L, 1L)).thenReturn(1);
        assertThat(transportHubService.updateCapacity(id, alarmCapacityChange)).isEqualTo(new ResponseHubDto(101L, "test-hub-1", newCapacity,
                "hub-1"));
        verify(outboxRepository, times(1)).insert(any(OutboxEntity.class));
    }

    @Test
    void testUpdateCapacityWithoutOutbox() {
        final long id = 101L;
        final int startCapacity = 20;
        final int noAlarmCapacityChange = MIN_CAPACITY_ALARM - startCapacity + 5;
        final int newCapacity = startCapacity + noAlarmCapacityChange;

        Mockito.when(transportHubRepository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", startCapacity, "hub-1", 0L)));

        Mockito.when(transportHubRepository.updateCapacity(id, newCapacity, 0L, 1L)).thenReturn(1);
        assertThat(transportHubService.updateCapacity(id, noAlarmCapacityChange)).isEqualTo(new ResponseHubDto(101L, "test-hub-1", newCapacity,
                "hub-1"));
        verify(outboxRepository, times(0)).insert(any(OutboxEntity.class));
    }

    @Test
    void testUpdateCapacityWhenNotEnough() {
        final long id = 101L;
        Mockito.when(transportHubRepository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", 20, "hub-1", 0L)));

        assertThatThrownBy(() -> transportHubService.updateCapacity(id, -30))
                .isInstanceOf(NotEnoughCapacityException.class)
                .hasMessage("Not enough capacity(%d) for request(%d) at hub id=%d, version=%d", 20, -30, id, 0);

        verify(outboxRepository, times(0)).insert(any(OutboxEntity.class));
    }

}