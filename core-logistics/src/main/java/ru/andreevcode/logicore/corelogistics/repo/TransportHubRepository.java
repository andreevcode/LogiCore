package ru.andreevcode.logicore.corelogistics.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.andreevcode.logicore.corelogistics.data.TransportHubEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransportHubRepository implements GeneralRepo<TransportHubEntity, Long> {
    private static final RowMapper<TransportHubEntity> ROW_MAPPER = ((rs, rowNum) -> new TransportHubEntity(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("capacity"),
            rs.getString("code"))
    );

    private static final String FIND_ALL = """
                SELECT id, name, capacity, code FROM logistics.transport_hub;
            """;

    private static final String FIND_BY_ID = """
                SELECT id, name, capacity, code FROM logistics.transport_hub where id = :id;
            """;

    private static final String INSERT = """
                INSERT INTO logistics.transport_hub (name, capacity, code) VALUES (:name, :capacity, :code);
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @Override
    public List<TransportHubEntity> findAll() {
        return namedParameterJdbcTemplate.query(FIND_ALL, ROW_MAPPER);
    }

    @Override
    public Optional<TransportHubEntity> findById(Long id) {
        try {
            return Optional.of(namedParameterJdbcTemplate.queryForObject(FIND_BY_ID, Map.of("id", id), ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    @Override
    public TransportHubEntity save(TransportHubEntity transportHub) {
        var keyHolder = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("name", transportHub.getName())
                .addValue("capacity", transportHub.getCapacity())
                .addValue("code", transportHub.getCode());
        namedParameterJdbcTemplate.update(INSERT, params, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated id was not returned");
        }
        transportHub.setId(key.longValue());
        return transportHub;
    }
}
