package com.projectsa.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "app.demo-users=true",
        "spring.datasource.url=jdbc:h2:mem:demo-users;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"})
class DemoUsersTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuthService auth;

    @Test
    void fiveDemoAccountsOneRoleEachAndLoginWorks() {
        assertEquals(List.of("PM", "Presale", "Sale", "TechEngineer", "TechEngineer"),
                jdbc.queryForList("SELECT Role FROM EMPLOYEE ORDER BY Role", String.class));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM ENGINEER_SKILL", Integer.class));
        assertEquals("TechEngineer", auth.login("tech1", DemoUsers.PASSWORD).role());
    }
}
