package com.projectsa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.projectsa.common.QueryCatalog;

@SpringBootTest
class ApplicationStartsTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    QueryCatalog catalog;

    @Test
    void schemaCreatedFromDesignFileAndSkillsSeeded() {
        assertEquals(87, catalog.all().size());
        assertEquals(11, jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", Integer.class));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class));
    }

    @Autowired
    com.projectsa.common.Sql sql;

    @Test
    void resultKeysKeepTheSchemaSpelling() {
        // JSON sent to the pages uses these keys, e.g. row.SkillName (not SKILLNAME)
        assertEquals(java.util.List.of("SkillID", "SkillName"),
                java.util.List.copyOf(sql.rows("F1", "R.1", java.util.Map.of()).get(0).keySet()));
    }
}
