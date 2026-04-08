package ru.andreevcode.logicore.routing_engine.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.andreevcode.logicore.routing_engine.kafka.data.HubsEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HubsRepository implements GeneralRepo<HubsEntity, Long> {
    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String CAPACITY = "capacity";
    private static final String CAPACITY_UPDATED_AT = "capacity_updated_at";

    private static final RowMapper<HubsEntity> ROW_MAPPER = ((rs, rowNum) ->
            new HubsEntity(
                    rs.getLong(ID),
                    rs.getString(CODE),
                    rs.getInt(CAPACITY),
                    rs.getTimestamp(CAPACITY_UPDATED_AT).toInstant()
            ));

    private static final String FIND_ALL_SQL = """
            SELECT id, code, capacity, capacity_updated_at FROM routing.hubs;
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, code, capacity, capacity_updated_at FROM routing.hubs
            WHERE id = :id;
            """;

    private static final String INSERT_SQL = """
            INSERT INTO routing.hubs (id, code, capacity, capacity_updated_at)
            VALUES (:id, :code, :capacity, :capacity_updated_at);
            """;

    private static final String UPSERT_CAPACITY_SQL = """
            INSERT INTO routing.hubs (id, code, capacity, capacity_updated_at)
            VALUES (:id, :code, :capacity, :capacity_updated_at)
            ON CONFLICT (id) DO UPDATE
                SET capacity = EXCLUDED.capacity, capacity_updated_at = EXCLUDED.capacity_updated_at
            WHERE  EXCLUDED.capacity_updated_at > hubs.capacity_updated_at;
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @Override
    public List<HubsEntity> findAll() {
        return namedParameterJdbcTemplate.query(FIND_ALL_SQL, ROW_MAPPER);
    }

    @Override
    public Optional<HubsEntity> findById(Long id) {
        try {
            return Optional.of(namedParameterJdbcTemplate.queryForObject(
                    FIND_BY_ID_SQL,
                    Map.of(ID, id),
                    ROW_MAPPER)
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public int insert(HubsEntity hubsEntity) {
        return namedParameterJdbcTemplate.update(INSERT_SQL, createFullParams(hubsEntity));

    }

    public int upsertCapacity(HubsEntity hubsEntity) {
        return namedParameterJdbcTemplate.update(UPSERT_CAPACITY_SQL, createFullParams(hubsEntity));
    }

    private MapSqlParameterSource createFullParams(HubsEntity hubsEntity) {
        return new MapSqlParameterSource()
                .addValue(ID, hubsEntity.getId())
                .addValue(CODE, hubsEntity.getCode())
                .addValue(CAPACITY, hubsEntity.getCapacity())
                .addValue(CAPACITY_UPDATED_AT, Timestamp.from(hubsEntity.getCapacityUpdatedAt()));
    }
}
