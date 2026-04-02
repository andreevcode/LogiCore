package ru.andreevcode.logicore.corelogistics.repo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventStatus;
import ru.andreevcode.logicore.corelogistics.outbox.OutboxEventType;
import ru.andreevcode.logicore.corelogistics.outbox.data.OutboxEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OutboxRepository {
    public static final RowMapper<OutboxEntity> OUTBOX_ROW_MAPPER = (rs, rowNum) ->
            new OutboxEntity(
                    rs.getLong("id"),
                    rs.getObject("external_id", UUID.class),
                    OutboxEventType.valueOf(rs.getString("event_type")),
                    rs.getString("aggregation_id"),
                    rs.getString("payload"),
                    OutboxEventStatus.valueOf(rs.getString("event_status")),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    rs.getInt("retry_count"),
                    rs.getString("last_error"),
                    rs.getString("trace_id")
            );

    private static final String SELECT_TO_PROCESS_SQL = """
                SELECT id, external_id, aggregation_id, event_type, payload, event_status, created_at, 
                       updated_at, retry_count, last_error, trace_id 
                FROM logistics.outbox 
                WHERE event_status = ANY(:event_statuses::logistics.outbox_event_status[]) 
                ORDER BY created_at LIMIT :limit
                FOR UPDATE SKIP LOCKED;
            """;
    public static final String INSERT_SQL = """
                INSERT INTO logistics.outbox (external_id, event_type, aggregation_id, payload, event_status, created_at, 
                                              updated_at, retry_count, last_error, trace_id) 
                VALUES (:external_id, :event_type::logistics.outbox_event_type, :aggregation_id, :payload::jsonb, 
                        :event_status::logistics.outbox_event_status, :created_at, 
                        :updated_at, :retry_count, :last_error, :trace_id)
                RETURNING id
            """;

    private static final String UPDATE_SQL = """
                UPDATE logistics.outbox 
                SET event_status = :event_status::logistics.outbox_event_status, 
                    updated_at = :updated_at,
                    retry_count = :retry_count,
                    last_error = :last_error,
                    trace_id = :trace_id
                WHERE id = :id
            """;

    private static final List<OutboxEventStatus> TO_PROCESS_STATUSES = List.of(OutboxEventStatus.NEW,
            OutboxEventStatus.FAILED);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<OutboxEntity> findToProcessTop(int limit) {
        Map<String, Object> params = Map.of(
                "event_statuses", TO_PROCESS_STATUSES.stream().map(Enum::name).toArray(String[]::new),
                "limit", limit
        );

        return namedParameterJdbcTemplate.query(SELECT_TO_PROCESS_SQL, params, OUTBOX_ROW_MAPPER);
    }

    public OutboxEntity insert(OutboxEntity outboxEntity) {
        var params = new MapSqlParameterSource()
                .addValue("external_id", outboxEntity.getExternalId())
                .addValue("event_type", outboxEntity.getEventType().name())
                .addValue("aggregation_id", outboxEntity.getAggregationId())
                .addValue("payload", outboxEntity.getPayload())
                .addValue("event_status", outboxEntity.getEventStatus().name())
                .addValue("created_at", Timestamp.from(outboxEntity.getCreatedAt()))
                .addValue("updated_at", Timestamp.from(outboxEntity.getUpdatedAt()))
                .addValue("retry_count", outboxEntity.getRetryCount())
                .addValue("last_error", outboxEntity.getLastError())
                .addValue("trace_id", outboxEntity.getTraceId());
        Long id = namedParameterJdbcTemplate.queryForObject(INSERT_SQL, params, Long.class);
        outboxEntity.setId(id);
        return outboxEntity;
    }

    public void batchUpdate(List<OutboxEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }

        SqlParameterSource[] batchParams = entities.stream()
                .map(entity -> new MapSqlParameterSource()
                        .addValue("id", entity.getId())
                        .addValue("event_status", entity.getEventStatus().name())
                        .addValue("updated_at", Timestamp.from(entity.getUpdatedAt()))
                        .addValue("retry_count", entity.getRetryCount())
                        .addValue("last_error", entity.getLastError())
                        .addValue("trace_id", entity.getTraceId())
                )
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(UPDATE_SQL, batchParams);
    }
}
