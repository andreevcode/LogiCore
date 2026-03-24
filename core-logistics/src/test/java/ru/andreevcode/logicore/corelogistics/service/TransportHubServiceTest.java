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
import ru.andreevcode.logicore.corelogistics.repo.TransportHubRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransportHubServiceTest {

    TransportHubService transportHubService;

    @Mock
    TransportHubRepository repository;

    @BeforeEach
    void init() {
        transportHubService = new TransportHubService(repository);
    }

    @Test
    void testInsertConversions() {
        var requestHubDto = new RequestHubDto("test-hub-1", 2, "hub-1");
        Mockito.when(repository.insert(any()))
                .thenReturn(new TransportHubEntity(101L, "test-hub-1", 2, "hub-1", 0L));

        Assertions.assertEquals(new ResponseHubDto(101L, "test-hub-1", 2, "hub-1"),
                transportHubService.insert(requestHubDto)
        );
    }

    @Test
    void testFindByIdConversions() {
        final long id = 101L;
        Mockito.when(repository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", 2, "hub-1", 0L)));

        assertThat(transportHubService.findById(id)).isEqualTo(new ResponseHubDto(101L, "test-hub-1", 2, "hub-1"));
    }

    @Test
    void shouldFindByIdThrowHubNotFoundException() {
        final long id = 101L;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transportHubService.findById(id))
                .isInstanceOf(HubNotFoundException.class)
                .hasMessage("No transport hub found for id=%d", id);
    }

    @Test
    void testUpdateCapacity() {
        final long id = 101L;
        Mockito.when(repository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", 20, "hub-1", 0L)));

        Mockito.when(repository.updateCapacity(id, 10, 0L, 1L)).thenReturn(1);
        assertThat(transportHubService.updateCapacity(id, -10)).isEqualTo(new ResponseHubDto(101L, "test-hub-1", 10,
                "hub-1"));
    }

    @Test
    void testUpdateCapacityWhenNotEnough() {
        final long id = 101L;
        Mockito.when(repository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1", 20, "hub-1", 0L)));

        assertThatThrownBy(() -> transportHubService.updateCapacity(id, -30))
                .isInstanceOf(NotEnoughCapacityException.class)
                .hasMessage("Not enough capacity(%d) for request(%d) at hub id=%d, version=%d", 20, -30, id, 0);
    }

}