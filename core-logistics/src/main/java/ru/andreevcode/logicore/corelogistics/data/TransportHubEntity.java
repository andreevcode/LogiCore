package ru.andreevcode.logicore.corelogistics.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransportHubEntity {
    private Long id;
    private String name;
    private Integer capacity;
    private String code;
}
