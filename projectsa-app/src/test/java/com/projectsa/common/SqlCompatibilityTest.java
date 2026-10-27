package com.projectsa.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Every statement of ProjectSA_SQL.sql must be accepted by H2 in MSSQLServer mode (CLAUDE.md §5 step 2). */
class SqlCompatibilityTest {

    @Test
    void schemaAndEveryQueryPrepareOnH2() throws Exception {
        QueryCatalog catalog = QueryCatalog.fromClasspath();
        List<String> failures = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:compat;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")) {
            try (Statement st = c.createStatement()) {
                for (String ddl : catalog.schemaStatements()) {
                    try {
                        st.execute(ddl);
                    } catch (Exception e) {
                        failures.add("SCHEMA: " + ddl.lines().findFirst().orElse("") + " -> " + e.getMessage());
                    }
                }
            }
            int statements = 0;
            for (QueryCatalog.Query q : catalog.all().values()) {
                for (String sql : q.statements()) {
                    statements++;
                    String jdbc = sql.replaceAll("(?<!:):(\\w+)", "?");
                    try {
                        c.prepareStatement(jdbc).close();
                    } catch (Exception e) {
                        failures.add(q.key() + " -> " + e.getMessage().lines().findFirst().orElse(""));
                    }
                }
            }
            System.out.println("queries: " + catalog.all().size() + ", statements: " + statements);
        }
        failures.forEach(f -> System.out.println("H2 FAIL  " + f));
        assertEquals(87, catalog.all().size(), "query count in ProjectSA_SQL.sql");
        assertTrue(failures.isEmpty(), failures.size() + " statement(s) rejected by H2");
    }
}
