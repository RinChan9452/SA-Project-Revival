package com.projectsa.uc04;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;
import com.projectsa.common.SessionUser;
import com.projectsa.uc01.ProjectDetectBlank.ProjectForm;
import com.projectsa.uc01.ProjectService;
import com.projectsa.uc02.SolutionDetectBlank.SolutionForm;
import com.projectsa.uc02.SolutionService;

/** CLAUDE.md §6.4 UC04 Assign Engineer to Project. */
@SpringBootTest
@AutoConfigureMockMvc
class AssignApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired JdbcTemplate jdbc;

    private ResultActions assign(long id, String json, MockHttpSession s) throws Exception {
        return mvc.perform(post("/api/projects/" + id + "/assignments").session(s)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void assignEngineers() throws Exception {
        long sale = TestData.register(auth, "u4_sale", Auth.SALE);
        long pre = TestData.register(auth, "u4_pre", Auth.PRESALE);
        long pm = TestData.register(auth, "u4_pm", Auth.PM);
        long ccie = jdbc.queryForObject("SELECT SkillID FROM SKILL WHERE SkillName = 'CCIE'", Long.class);
        long java = jdbc.queryForObject("SELECT SkillID FROM SKILL WHERE SkillName = 'Java'", Long.class);
        long t1 = TestData.register(auth, "u4_t1", Auth.TECH, "CCIE");
        long t2 = TestData.register(auth, "u4_t2", Auth.TECH, "Java");
        long id = uc01.create(new SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC04 Project", null, "UC04 Co", "c@uc04.com", pre, "Site"));
        MockHttpSession pmS = TestData.login(mvc, "u4_pm");

        // not yet WaitingForAssignEngineer
        mvc.perform(get("/api/projects/" + id + "/assign-form").session(pmS)).andExpect(status().isConflict());
        uc02.save(new SessionUser(pre, Auth.PRESALE, "a", "b", null), id, new SolutionForm("2026-11-01", "2026-12-01", "O", "10", null),
                List.of(new MockMultipartFile("f", "s.pdf", "application/pdf", "x".getBytes())), List.of());

        mvc.perform(get("/api/projects?status=WaitingForAssignEngineer").session(pmS))
                .andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").exists());
        mvc.perform(get("/api/projects/" + id + "/assign-form").session(TestData.login(mvc, "u4_sale"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/assign-form").session(pmS))
                .andExpect(jsonPath("$.project.Objective").value("O"))
                .andExpect(jsonPath("$.engineers[?(@.EmployeeID == " + t1 + ")].Skills").value("CCIE"))
                .andExpect(jsonPath("$.engineers[?(@.EmployeeID == " + t1 + ")].InThisProject").value(0))
                .andExpect(jsonPath("$.skills.length()").value(jdbc.queryForObject("SELECT COUNT(*) FROM SKILL", Integer.class)));
        // 5.1 filter, Alt 5a
        mvc.perform(get("/api/engineers?skill=" + java).session(pmS)).andExpect(jsonPath("$[?(@.EmployeeID == " + t2 + ")]").exists())
                .andExpect(jsonPath("$[?(@.EmployeeID == " + t1 + ")]").doesNotExist());
        long unix = jdbc.queryForObject("SELECT SkillID FROM SKILL WHERE SkillName = 'Unix'", Long.class);
        int unixEngineers = jdbc.queryForObject("SELECT COUNT(*) FROM ENGINEER_SKILL WHERE SkillID = ?", Integer.class, unix);
        mvc.perform(get("/api/engineers?skill=" + unix).session(pmS)).andExpect(jsonPath("$.length()").value(unixEngineers));

        // Alt 7a, non-engineer id
        assign(id, "{\"engineerIds\":[]}", pmS).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.engineerIds").exists());
        assign(id, "{\"engineerIds\":[" + sale + "]}", pmS).andExpect(status().isBadRequest());

        assign(id, "{\"engineerIds\":[" + t1 + "," + t2 + "," + t1 + "]}", pmS).andExpect(status().isOk());
        assertEquals("Working", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        assertEquals(pm, jdbc.queryForObject("SELECT PMID FROM PROJECT WHERE ProjectID=?", Long.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_ASSIGNMENT WHERE ProjectID=?", Integer.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATION WHERE ProjectID=? AND Type='Assigned'", Integer.class, id));
        assign(id, "{\"engineerIds\":[" + t1 + "]}", pmS).andExpect(status().isConflict());     // no longer waiting
    }
}
