package com.projectsa.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    QueryCatalog queryCatalog() {
        return QueryCatalog.fromClasspath();
    }

    /** Built once the database is ready: on first start every table is created from the design's SQL file
     *  and SKILL is seeded (data.sql), so anything that uses Sql can rely on the schema. */
    @Bean
    Sql sql(NamedParameterJdbcTemplate jdbc, QueryCatalog catalog, DataSource ds) {
        initialise(new JdbcTemplate(ds), catalog);
        return new Sql(jdbc, catalog);
    }

    static void initialise(JdbcTemplate jdbc, QueryCatalog catalog) {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'EMPLOYEE'", Integer.class);
        if (tables == null || tables == 0) {
            catalog.schemaStatements().forEach(jdbc::execute);
            log.info("Created {} tables from ProjectSA_SQL.sql", catalog.schemaStatements().size());
        }
        Integer skills = jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class);
        if (skills == null || skills == 0) {
            for (String st : readResource("/data.sql").split(";")) {
                String s = st.lines().filter(l -> !l.strip().startsWith("--")).reduce("", (a, b) -> a + b + "\n").strip();
                if (!s.isEmpty()) {
                    jdbc.execute(s);
                }
            }
            log.info("Seeded SKILL from data.sql");
        }
    }

    private static String readResource(String path) {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
