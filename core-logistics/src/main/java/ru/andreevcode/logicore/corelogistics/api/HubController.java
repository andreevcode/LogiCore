package ru.andreevcode.logicore.corelogistics.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.andreevcode.logicore.corelogistics.data.RequestChangeCapacityDto;
import ru.andreevcode.logicore.corelogistics.data.RequestHubDto;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.service.TransportHubService;

@RestController
@RequestMapping(path = "/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {
    private final TransportHubService transportHubService;

    @GetMapping
    public Iterable<ResponseHubDto> getTransportHubs() {
        return transportHubService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseHubDto> getTransportHub(@PathVariable Long id) {
        return ResponseEntity.ok(transportHubService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResponseHubDto> postTransportHub(@Valid @RequestBody RequestHubDto newHub) {
        return new ResponseEntity<>(transportHubService.insert(newHub), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/capacity")
    public ResponseEntity<ResponseHubDto> updateCapacity(
            @PathVariable long id,
            @Valid @RequestBody RequestChangeCapacityDto dto) {
        return new ResponseEntity<>(transportHubService.updateCapacity(id, dto.amount()), HttpStatus.ACCEPTED);
    }
}
