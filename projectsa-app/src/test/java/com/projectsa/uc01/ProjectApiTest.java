package com.projectsa.uc01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;

/** CLAUDE.md §6.1 UC01 Create Project Profile. */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;

    private ResultActions create(MockHttpSession s, String json) throws Exception {
        return mvc.perform(post("/api/projects").session(s).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void createProjectProfile() throws Exception {
        TestData.register(auth, "u1_sale", Auth.SALE);
        long presale = TestData.register(auth, "u1_pre", Auth.PRESALE);
        TestData.register(auth, "u1_pm", Auth.PM);
        MockHttpSession sale = TestData.login(mvc, "u1_sale");
        MockHttpSession pm = TestData.login(mvc, "u1_pm");

        mvc.perform(get("/api/projects/create-form").session(pm)).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/create-form").session(sale)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sale.FirstName").value("u1_sale"))            // 2.1 Sale in Charge = login
                .andExpect(jsonPath("$.presales[?(@.EmployeeID == " + presale + ")]").exists());

        // Alt 4a: 4.1–4.5 field errors
        create(sale, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.projectName").exists())
                .andExpect(jsonPath("$.fields.newCustomerName").exists())
                .andExpect(jsonPath("$.fields.newCustomerEmail").exists())
                .andExpect(jsonPath("$.fields.presaleId").exists())
                .andExpect(jsonPath("$.fields.projectSite").exists());
        create(sale, """
                {"projectName":"P","newCustomerName":"C","newCustomerEmail":"bad","presaleId":%d,"projectSite":"S"}"""
                .formatted(presale)).andExpect(jsonPath("$.fields.newCustomerEmail").value("รูปแบบอีเมลไม่ถูกต้อง"));
        create(sale, """
                {"projectName":"P","newCustomerName":"C","newCustomerEmail":"c@c.com","presaleId":999999,"projectSite":"S"}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.presaleId").exists());
        create(pm, "{}").andExpect(status().isForbidden());

        // new customer → 5.1 + 5.2 + 7.1
        String body = create(sale, """
                {"projectName":"UC01 Network","newCustomerName":"UC01 Co","newCustomerEmail":"it@uc01.com",
                 "presaleId":%d,"projectSite":"Bangkok"}""".formatted(presale))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll("\\D", ""));
        assertEquals("NewProject", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID = ?", String.class, id));
        assertEquals(presale, jdbc.queryForObject("SELECT PresaleID FROM PROJECT WHERE ProjectID = ?", Long.class, id));
        assertEquals("มี Project ใหม่รอออกแบบ Solution: UC01 Network", jdbc.queryForObject(
                "SELECT Message FROM NOTIFICATION WHERE ProjectID = ? AND RecipientID = ?", String.class, id, presale));

        // existing customer → no new CUSTOMER row
        long cust = jdbc.queryForObject("SELECT CustomerID FROM CUSTOMER WHERE CustomerName = 'UC01 Co'", Long.class);
        int customers = jdbc.queryForObject("SELECT COUNT(*) FROM CUSTOMER", Integer.class);
        create(sale, """
                {"projectName":"UC01 Second","customerId":%d,"presaleId":%d,"projectSite":"Chonburi"}"""
                .formatted(cust, presale)).andExpect(status().isCreated());
        assertEquals(customers, jdbc.queryForObject("SELECT COUNT(*) FROM CUSTOMER", Integer.class));
    }
}
