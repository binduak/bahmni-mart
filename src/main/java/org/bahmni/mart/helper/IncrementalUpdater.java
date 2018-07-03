package org.bahmni.mart.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Component
public class IncrementalUpdater {

    private static final String EVENT_RECORD_ID = "event_record_id";
    private static final String CATEGORY = "category";
    private static final String TABLE_NAME = "table_name";
    private static final String QUERY_FOR_UUID_EXTRACTION = "SELECT DISTINCT substring_index(substring_index(object, " +
            "'/', -1), '?', 1) as uuid FROM event_records WHERE id > %s AND category = '%s'";
    private static final String QUERY_FOR_ID_EXTRACTION = "SELECT %s_id FROM %s WHERE uuid in (%s)";
    private static final String UPDATED_READER_SQL = "SELECT * FROM ( %s ) result WHERE %s IN (%s)";
    private static final String NON_EXISTED_ID = "-1";
    public static final String QUERY_FOR_MAX_EVENT_RECORD_ID = "SELECT MAX(id) FROM event_records";

    @Autowired
    @Qualifier("openmrsJdbcTemplate")
    private JdbcTemplate openmrsJdbcTemplate;

    @Autowired
    @Qualifier("martJdbcTemplate")
    private JdbcTemplate martJdbcTemplate;

    @Autowired
    private MarkerMapper markerMapper;

    private String maxEventRecordId;

    public String updateReaderSql(String readerSql, String jobName, String updateOn) {
        Optional<Map<String, Object>> optionalMarkerMap = markerMapper.getJobMarkerMap(jobName);
        if (!optionalMarkerMap.isPresent() || optionalMarkerMap.get().get(EVENT_RECORD_ID).equals(0)) {
            return readerSql;
        }
        String joinedIds = getJoinedIds(optionalMarkerMap.get());
        return String.format(UPDATED_READER_SQL, readerSql, updateOn, joinedIds);
    }

    private String getJoinedIds(Map<String, Object> markerMap) {
        String eventRecordId = String.valueOf(markerMap.get(EVENT_RECORD_ID));
        String category = (String) markerMap.get(CATEGORY);
        String tableName = (String) markerMap.get(TABLE_NAME);
        List<String> uuids = getEventRecordUuids(eventRecordId, category);
        if (uuids.isEmpty()) {
            return NON_EXISTED_ID;
        }

        return getIdListFor(tableName, uuids).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<String> getEventRecordUuids(String eventRecordId, String category) {
        String queryForEventRecordObjects = String.format(QUERY_FOR_UUID_EXTRACTION, eventRecordId, category);
        return openmrsJdbcTemplate.queryForList(queryForEventRecordObjects, String.class);
    }

    private List<String> getIdListFor(String tableName, List<String> uuids) {
        String joinedUuids = uuids.stream().map(uuid -> String.format("'%s'", uuid)).collect(Collectors.joining(","));
        return openmrsJdbcTemplate.queryForList(String.format(QUERY_FOR_ID_EXTRACTION, tableName, tableName,
                joinedUuids), String.class);
    }

    public void deleteVoidedRecords(Set<String> ids, String table, String column) {
        if (isNull(ids) || ids.isEmpty()) {
            return;
        }
        String deleteSql = String.format("DELETE FROM %s WHERE %s IN (%s)", table, column, String.join(",", ids));
        martJdbcTemplate.execute(deleteSql);
    }

    public void updateMarker(String jobName) {
        maxEventRecordId = getMaxEventRecordId();
        if (isNull(maxEventRecordId)) {
            maxEventRecordId = String.valueOf(0);
        }
        markerMapper.updateMarker(jobName, maxEventRecordId);
    }

    private String getMaxEventRecordId() {
        if (Objects.nonNull(maxEventRecordId)) {
            return maxEventRecordId;
        }
        return openmrsJdbcTemplate.queryForObject(QUERY_FOR_MAX_EVENT_RECORD_ID, String.class);
    }
}