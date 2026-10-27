package com.projectsa.common;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Runs a design query ("UC04", "8.1") with bound parameters. Never concatenates SQL.
 * Parameters a statement does not use are ignored, so callers can pass one map per step.
 */
public class Sql {

    private static final Logger log = LoggerFactory.getLogger(Sql.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final QueryCatalog catalog;
    private Consumer<QueryCatalog.Query> listener = q -> { };

    public Sql(NamedParameterJdbcTemplate jdbc, QueryCatalog catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    public QueryCatalog catalog() {
        return catalog;
    }

    /** Called with every query that runs (tests use it to check that all design queries are exercised). */
    public void setListener(Consumer<QueryCatalog.Query> listener) {
        this.listener = listener;
    }

    private QueryCatalog.Query use(String section, String id) {
        QueryCatalog.Query q = catalog.get(section, id);
        log.debug("Query {} {}", section, id);
        listener.accept(q);
        return q;
    }

    /** SELECT query → rows (case-insensitive column keys). */
    public List<Map<String, Object>> rows(String section, String id, Map<String, ?> params) {
        QueryCatalog.Query q = use(section, id);
        List<Map<String, Object>> result = List.of();
        for (String st : q.statements()) {
            if (st.regionMatches(true, 0, "SELECT", 0, 6)) {
                result = jdbc.queryForList(st, source(params));
            } else {
                jdbc.update(st, source(params));
            }
        }
        return result;
    }

    public Map<String, Object> row(String section, String id, Map<String, ?> params) {
        List<Map<String, Object>> r = rows(section, id, params);
        return r.isEmpty() ? null : r.get(0);
    }

    /** First column of the first row (COUNT(*), Status, …). */
    public Object value(String section, String id, Map<String, ?> params) {
        Map<String, Object> r = row(section, id, params);
        return r == null ? null : r.values().iterator().next();
    }

    public long count(String section, String id, Map<String, ?> params) {
        Object v = value(section, id, params);
        return v == null ? 0 : ((Number) v).longValue();
    }

    /** INSERT / UPDATE / DELETE → total rows affected (all statements of the query). */
    public int update(String section, String id, Map<String, ?> params) {
        int n = 0;
        for (String st : use(section, id).statements()) {
            n += jdbc.update(st, source(params));
        }
        return n;
    }

    /** INSERT whose design text ends with "SET @X = SCOPE_IDENTITY()" → the new identity value. */
    public long insert(String section, String id, Map<String, ?> params) {
        QueryCatalog.Query q = use(section, id);
        if (q.identityParam() == null || q.statements().size() != 1) {
            throw new IllegalStateException(q.key() + " does not return an identity");
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(q.statements().get(0), source(params), keys, new String[] {q.identityParam()});
        return keys.getKey().longValue();
    }

    private static MapSqlParameterSource source(Map<String, ?> params) {
        MapSqlParameterSource s = new MapSqlParameterSource();
        params.forEach(s::addValue);
        return s;
    }
}
