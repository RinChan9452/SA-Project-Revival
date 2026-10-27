package com.projectsa.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import com.projectsa.common.Auth;

/**
 * CLAUDE.md §5 step 3: demo accounts (1 Sale, 1 Presale, 1 PM, 2 Tech Engineers) registered through F1
 * on first start when app.demo-users=true. Password for all: demo1234.
 */
@Configuration
public class DemoUsers {

    private static final Logger log = LoggerFactory.getLogger(DemoUsers.class);
    public static final String PASSWORD = "demo1234";

    @Bean
    @Order(10)
    @ConditionalOnProperty(name = "app.demo-users", havingValue = "true")
    ApplicationRunner demoUsersRunner(AuthService auth, JdbcTemplate jdbc) {
        return args -> {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE", Integer.class);
            if (n != null && n > 0) {
                return;
            }
            reg(auth, "สมชาย", "ขายเก่ง", "sale", Auth.SALE, List.of());
            reg(auth, "พิมพ์", "ออกแบบดี", "presale", Auth.PRESALE, List.of());
            reg(auth, "กานต์", "บริหารงาน", "pm", Auth.PM, List.of());
            reg(auth, "ต้น", "ช่างเน็ต", "tech1", Auth.TECH, List.of("CCIE", "CCNA"));
            reg(auth, "นก", "ช่างระบบ", "tech2", Auth.TECH, List.of("Java", "Linux"));
            log.info("Demo users created (password {}): sale, presale, pm, tech1, tech2", PASSWORD);
        };
    }

    private static void reg(AuthService auth, String first, String last, String user, String role, List<String> skills) {
        auth.register(new RegisterDetectBlank.RegisterForm(first, last, user + "@demo.local", null, user, PASSWORD, role, skills));
    }
}
