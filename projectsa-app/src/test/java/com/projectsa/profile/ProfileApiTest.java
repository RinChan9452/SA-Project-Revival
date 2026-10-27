package com.projectsa.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;

/** CLAUDE.md §6.0 F3 Profile. */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;

    @Test
    void editProfile() throws Exception {
        long tech = TestData.register(auth, "f3_tech", Auth.TECH, "CCIE", "CCNA");
        TestData.register(auth, "f3_other", Auth.SALE);
        MockHttpSession s = TestData.login(mvc, "f3_tech");
        MockHttpSession sale = TestData.login(mvc, "f3_other");

        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile").session(s))
                .andExpect(jsonPath("$.me.Role").value("TechEngineer"))
                .andExpect(jsonPath("$.me.Username").value("f3_tech"))
                .andExpect(jsonPath("$.skills.length()").value(2))
                .andExpect(jsonPath("$.allSkills.length()").value(jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class)));
        mvc.perform(get("/api/profile").session(sale)).andExpect(jsonPath("$.skills").doesNotExist());

        // info: required / format / unique email; role cannot be changed (not a field)
        mvc.perform(put("/api/profile").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.email").value("รูปแบบอีเมลไม่ถูกต้อง"));
        mvc.perform(put("/api/profile").session(s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"T\",\"lastName\":\"N\",\"email\":\"f3_other@t.local\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/profile").session(s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Tonny\",\"lastName\":\"Tech\",\"email\":\"tonny@t.local\",\"phone\":\"0812345678\",\"role\":\"PM\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me").session(s)).andExpect(jsonPath("$.Name").value("Tonny Tech")).andExpect(jsonPath("$.Role").value("TechEngineer"));
        assertEquals("TechEngineer", jdbc.queryForObject("SELECT Role FROM EMPLOYEE WHERE EmployeeID=?", String.class, tech));

        // picture: rules, then public URL serves the file, header gets it
        mvc.perform(multipart("/api/profile/picture").file(new MockMultipartFile("picture", "me.pdf", "application/pdf", "x".getBytes())).session(s))
                .andExpect(status().isBadRequest());
        String body = mvc.perform(multipart("/api/profile/picture")
                        .file(new MockMultipartFile("picture", "me.png", "image/png", "PNGDATA".getBytes())).session(s))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url = body.replaceAll(".*\"ProfilePicture\":\"([^\"]+)\".*", "$1");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes("PNGDATA".getBytes()));
        mvc.perform(get("/api/me").session(s)).andExpect(jsonPath("$.ProfilePicture").value(url));

        // password: current must match; then login works only with the new one
        mvc.perform(put("/api/profile/password").session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpass1\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/profile/password").session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"secret1\",\"newPassword\":\"newpass1\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"f3_tech\",\"password\":\"newpass1\"}")).andExpect(status().isOk());

        // skills: Tech only, typed by the engineer, at least 1, replaced in one go
        mvc.perform(put("/api/profile/skills").session(sale).contentType(MediaType.APPLICATION_JSON).content("{\"skills\":[\"Java\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/profile/skills").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"skills\":[\" \"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.skills").exists());
        mvc.perform(put("/api/profile/skills").session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"skills\":[\"" + "x".repeat(51) + "\"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.skills").exists());
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class);
        // "java" / "JAVA" = the existing Java (R.5); "F3 Ansible" is new (R.6) and added once
        mvc.perform(put("/api/profile/skills").session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"skills\":[\"java\",\"  F3   Ansible \",\"JAVA\",\"f3 ansible\"]}"))
                .andExpect(status().isOk());
        assertEquals(List.of("F3 Ansible", "Java"), jdbc.queryForList(
                "SELECT s.SkillName FROM ENGINEER_SKILL es JOIN SKILL s ON s.SkillID = es.SkillID WHERE es.EmployeeID=? ORDER BY s.SkillName",
                String.class, tech));
        assertEquals(before + 1, jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class));
        mvc.perform(get("/api/skills")).andExpect(jsonPath("$[?(@.SkillName == 'F3 Ansible')]").exists());
    }
}
