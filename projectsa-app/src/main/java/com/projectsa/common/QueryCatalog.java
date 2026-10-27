package com.projectsa.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads "Project Source/ProjectSA_SQL.sql" (copied to classpath:sql/ by Maven) and exposes the schema
 * and every "Query N.M" of every section (UC01..UC08, F1..F4).
 * The app never contains its own SQL text: it runs exactly what the design documents say.
 */
public final class QueryCatalog {

    /** One query of the design, already converted for JDBC. */
    public record Query(String section, String id, String note, List<String> statements, String identityParam) {
        public String key() {
            return section + " " + id;
        }
    }

    private static final Pattern SECTION = Pattern.compile("^-- ((?:UC\\d\\d|F\\d)) .*$");
    private static final Pattern QUERY = Pattern.compile("^-- Query ([\\w.]+)(?:: (.*))?$");
    private static final Pattern SET_IDENTITY = Pattern.compile("SET @(\\w+) = SCOPE_IDENTITY\\(\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAM = Pattern.compile("(?<![@\\w])@(\\w+)");

    private final String schema;
    private final Map<String, Query> queries;

    private QueryCatalog(String schema, Map<String, Query> queries) {
        this.schema = schema;
        this.queries = Collections.unmodifiableMap(queries);
    }

    public static QueryCatalog fromClasspath() {
        try (InputStream in = QueryCatalog.class.getResourceAsStream("/sql/ProjectSA_SQL.sql")) {
            if (in == null) {
                throw new IllegalStateException("classpath:sql/ProjectSA_SQL.sql missing (see pom.xml resources)");
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static QueryCatalog parse(String text) {
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        text = text.replace("\r\n", "\n");
        int first = text.indexOf("\n-- ====");
        String schema = text.substring(0, first);
        Map<String, Query> out = new LinkedHashMap<>();
        String section = null, id = null, note = null;
        StringBuilder body = new StringBuilder();
        for (String line : text.substring(first + 1).split("\n", -1)) {
            Matcher s = SECTION.matcher(line);
            Matcher q = QUERY.matcher(line);
            boolean bar = line.startsWith("-- ====");
            if (s.matches() || q.matches() || bar) {
                if (id != null) {
                    put(out, section, id, note, body.toString());
                    id = null;
                }
                if (s.matches()) {
                    section = s.group(1);
                } else if (q.matches()) {
                    id = q.group(1);
                    note = q.group(2) == null ? "" : q.group(2);
                    body.setLength(0);
                }
                continue;
            }
            if (id != null) {
                body.append(line).append('\n');
            }
        }
        if (id != null) {
            put(out, section, id, note, body.toString());
        }
        return new QueryCatalog(schema, out);
    }

    private static void put(Map<String, Query> out, String section, String id, String note, String sql) {
        String body = sql.strip();
        List<String> statements = new ArrayList<>();
        String identity = null;
        if (!body.equals("-- (no statement)")) {
            for (String raw : body.split(";")) {
                String st = raw.strip();
                if (st.isEmpty()) {
                    continue;
                }
                Matcher m = SET_IDENTITY.matcher(st);
                if (m.matches()) {
                    identity = m.group(1);          // returned through JDBC generated keys instead
                    continue;
                }
                statements.add(PARAM.matcher(st).replaceAll(":$1"));
            }
        }
        out.put(section + " " + id, new Query(section, id, note, List.copyOf(statements), identity));
    }

    public Query get(String section, String id) {
        Query q = queries.get(section + " " + id);
        if (q == null) {
            throw new IllegalArgumentException("No Query " + id + " in section " + section + " of ProjectSA_SQL.sql");
        }
        return q;
    }

    public Map<String, Query> all() {
        return queries;
    }

    /** CREATE TABLE statements, in file order. */
    public List<String> schemaStatements() {
        List<String> out = new ArrayList<>();
        for (String raw : schema.split(";")) {
            String st = raw.lines().filter(l -> !l.strip().startsWith("--")).reduce("", (a, b) -> a + b + "\n").strip();
            if (!st.isEmpty()) {
                out.add(st);
            }
        }
        return out;
    }
}
