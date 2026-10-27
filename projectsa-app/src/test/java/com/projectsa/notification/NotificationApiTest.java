package com.projectsa.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;

/** CLAUDE.md §6.0 F4 Notification Bell. */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;

    @Test
    void countListReadAndReadAllOnlyForDueNotificationsOfTheUser() throws Exception {
        long sale = TestData.register(auth, "bell_sale", Auth.SALE);
        long pre = TestData.register(auth, "bell_pre", Auth.PRESALE);
        jdbc.update("INSERT INTO CUSTOMER (CustomerName, CustomerEmail) VALUES ('Bell Co', 'b@c.com')");
        long cust = jdbc.queryForObject("SELECT MAX(CustomerID) FROM CUSTOMER", Long.class);
        jdbc.update("INSERT INTO PROJECT (ProjectName, ProjectSite, CustomerID, SaleID, PresaleID) VALUES ('Bell P','x',?,?,?)",
                cust, sale, pre);
        long project = jdbc.queryForObject("SELECT MAX(ProjectID) FROM PROJECT", Long.class);
        String ins = "INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead) VALUES (?,?,?,?,?,0)";
        jdbc.update(ins, pre, project, "NewProject", "older", Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
        jdbc.update(ins, pre, project, "NewProject", "newer", Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)));
        jdbc.update(ins, pre, project, "TestReminder", "future", Timestamp.valueOf(LocalDateTime.now().plusDays(3)));
        jdbc.update(ins, sale, project, "RequirementPending", "for sale", Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)));

        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        MockHttpSession s = TestData.login(mvc, "bell_pre");
        mvc.perform(get("/api/notifications/unread-count").session(s)).andExpect(jsonPath("$.Unread").value(2));
        String body = mvc.perform(get("/api/notifications").session(s))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].Message").value("newer"))
                .andExpect(jsonPath("$[0].ProjectName").value("Bell P"))
                .andReturn().getResponse().getContentAsString();
        long newest = Long.parseLong(body.replaceAll("(?s).*?\"NotificationID\":(\\d+).*", "$1"));

        mvc.perform(put("/api/notifications/" + newest + "/read").session(s)).andExpect(jsonPath("$.updated").value(1));
        mvc.perform(get("/api/notifications/unread-count").session(s)).andExpect(jsonPath("$.Unread").value(1));

        MockHttpSession other = TestData.login(mvc, "bell_sale");
        mvc.perform(put("/api/notifications/" + newest + "/read").session(other)).andExpect(jsonPath("$.updated").value(0));
        mvc.perform(put("/api/notifications/read-all").session(s)).andExpect(jsonPath("$.updated").value(1));
        mvc.perform(get("/api/notifications/unread-count").session(s)).andExpect(jsonPath("$.Unread").value(0));
        mvc.perform(get("/api/notifications/unread-count").session(other)).andExpect(jsonPath("$.Unread").value(1));
    }
}
