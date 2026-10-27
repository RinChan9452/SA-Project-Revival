package com.projectsa.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** CLAUDE.md §6.0 F1 Register + F2 Login. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    private static String reg(String user, String role, String skills) {
        return """
                {"firstName":"A","lastName":"B","email":"%s@x.com","phone":"","username":"%s",
                 "password":"secret1","role":"%s","skills":%s}""".formatted(user, user, role, skills);
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void skillsListComesFromSeed() throws Exception {
        mvc.perform(get("/api/skills")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].SkillName").value("CCIE"));
    }

    @Test
    void registerChecksFieldsAndOneRole() throws Exception {
        postJson("/api/register", "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.role").value("กรุณาเลือกบทบาท 1 บทบาท"));
        postJson("/api/register", reg("dual", "Sale,PM", "[]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.role").exists());
        postJson("/api/register", reg("tech0", "TechEngineer", "[]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.skills").exists());
        postJson("/api/register", reg("tech0", "TechEngineer", "[\"\", \"  \"]")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.skills").exists());
        postJson("/api/register", reg("bad@", "Sale", "[]").replace("bad@@x.com", "not-an-email"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void registerLoginMeLogout() throws Exception {
        postJson("/api/register", reg("auth_pm", "PM", "[]")).andExpect(status().isCreated());
        postJson("/api/register", reg("auth_pm", "PM", "[]")).andExpect(status().isConflict());
        // typed skills: "ccie" matches the seeded CCIE (R.5), "F1 Kubernetes" is new (R.6) and becomes a suggestion (R.1)
        postJson("/api/register", reg("auth_tech", "TechEngineer", "[\"ccie\", \"F1 Kubernetes\"]")).andExpect(status().isCreated());
        mvc.perform(get("/api/skills")).andExpect(jsonPath("$[?(@.SkillName == 'F1 Kubernetes')]").exists())
                .andExpect(jsonPath("$[?(@.SkillName == 'ccie')]").doesNotExist());

        postJson("/api/login", "{\"username\":\"auth_pm\",\"password\":\"wrong\"}").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        MockHttpSession session = (MockHttpSession) postJson("/api/login", "{\"username\":\"auth_pm\",\"password\":\"secret1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Role").value("PM"))
                .andExpect(jsonPath("$.menu.length()").value(4))
                .andExpect(jsonPath("$.menu[3].uc").value("UC08"))
                .andReturn().getRequest().getSession();
        mvc.perform(get("/api/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.Name").value("A B"));
        mvc.perform(post("/api/logout").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }
}
