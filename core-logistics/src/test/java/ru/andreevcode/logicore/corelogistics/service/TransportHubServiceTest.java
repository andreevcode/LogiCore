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
import ru.andreevcode.logicore.corelogistics.repo.TransportHubRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

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
    void testSaveConversions() {
        var requestHubDto = new RequestHubDto("test-hub-1", 2, "hub-1");
        Mockito.when(repository.save(any()))
                .thenReturn(new TransportHubEntity(101L, "test-hub-1",2, "hub-1"));

        Assertions.assertEquals(new ResponseHubDto(101L, "test-hub-1",2, "hub-1"),
                transportHubService.save(requestHubDto)
        );
    }

    @Test
    void testFindByIdConversions() {
        final long id = 101L;
        Mockito.when(repository.findById(id))
                .thenReturn(Optional.of(new TransportHubEntity(101L, "test-hub-1",2, "hub-1")));

        assertThat(transportHubService.findById(id)).isEqualTo(new ResponseHubDto(101L, "test-hub-1",2, "hub-1"));
    }

    @Test
    void shouldThrowHubNotFoundException() {
        final long id = 101L;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transportHubService.findById(id))
                .isInstanceOf(HubNotFoundException.class)
                .hasMessage("No transport hub found for id=%d", id);
    }

}