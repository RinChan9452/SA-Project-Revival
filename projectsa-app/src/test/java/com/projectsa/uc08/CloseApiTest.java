package com.projectsa.uc08;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;
import com.projectsa.common.SessionUser;
import com.projectsa.uc01.ProjectDetectBlank.ProjectForm;
import com.projectsa.uc01.ProjectService;
import com.projectsa.uc02.SolutionDetectBlank.SolutionForm;
import com.projectsa.uc02.SolutionService;
import com.projectsa.uc04.AssignDetectBlank.AssignForm;
import com.projectsa.uc04.AssignService;
import com.projectsa.uc05.ScheduleDetectBlank.ScheduleForm;
import com.projectsa.uc05.ScheduleService;

/** CLAUDE.md §6.9 UC08 Close Project. */
@SpringBootTest
@AutoConfigureMockMvc
class CloseApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired AssignService uc04;
    @Autowired ScheduleService uc05;
    @Autowired JdbcTemplate jdbc;

    @Test
    void closeProject() throws Exception {
        long sale = TestData.register(auth, "u8_sale", Auth.SALE);
        long pre = TestData.register(auth, "u8_pre", Auth.PRESALE);
        long pm = TestData.register(auth, "u8_pm", Auth.PM);
        TestData.register(auth, "u8_pm2", Auth.PM);
        long t1 = TestData.register(auth, "u8_t1", Auth.TECH, "CCIE");
        long t2 = TestData.register(auth, "u8_t2", Auth.TECH, "CCNA");
        SessionUser pmU = new SessionUser(pm, Auth.PM, "a", "b", null);
        long id = uc01.create(new SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC08 Project", null, "UC08 Co", "c@uc08.com", pre, "Site"));
        uc02.save(new SessionUser(pre, Auth.PRESALE, "a", "b", null), id, new SolutionForm("2026-11-01", "2026-12-01", "O", "10", "S"),
                List.of(new MockMultipartFile("f", "s.pdf", "application/pdf", "x".getBytes())), List.of());
        uc04.assign(pmU, id, new AssignForm(List.of(t1, t2)));
        MockHttpSession s = TestData.login(mvc, "u8_pm");

        // Working, not Testing yet → not listed; close refused (Alt 5a)
        mvc.perform(get("/api/projects?status=Testing&pm=me").session(s)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").doesNotExist());
        mvc.perform(post("/api/projects/" + id + "/close").session(s)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ไม่สามารถปิด Project ได้: Project ไม่ได้อยู่ในสถานะ Testing"));

        LocalDate test = LocalDate.now().plusDays(3);
        uc05.save(new SessionUser(t1, Auth.TECH, "a", "b", null), id, new ScheduleForm(test.toString(), "13:00", "15:00", "Site", null, 60));
        mvc.perform(get("/api/projects?status=Testing&pm=me").session(s)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").exists());
        mvc.perform(get("/api/projects/" + id + "/close-form").session(TestData.login(mvc, "u8_sale"))).andExpect(status().isForbidden());
        MockHttpSession otherPm = TestData.login(mvc, "u8_pm2");
        mvc.perform(get("/api/projects/" + id + "/close-form").session(otherPm)).andExpect(status().isConflict());
        mvc.perform(post("/api/projects/" + id + "/close").session(otherPm)).andExpect(status().isConflict());
        mvc.perform(get("/api/projects/" + id + "/close-form").session(s))
                .andExpect(jsonPath("$.project.SaleInCharge").value("u8_sale Sale"))
                .andExpect(jsonPath("$.engineers.length()").value(2))
                .andExpect(jsonPath("$.latestTest.TestDate").value(test.toString()));

        mvc.perform(post("/api/projects/" + id + "/close").session(s)).andExpect(status().isOk());
        assertEquals("Finish", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        assertEquals("Done", jdbc.queryForObject("SELECT Status FROM TEST_SCHEDULE WHERE ProjectID=?", String.class, id));
        assertEquals(List.of(sale, pre, t1, t2).stream().sorted().toList(), jdbc.queryForList(
                "SELECT RecipientID FROM NOTIFICATION WHERE ProjectID=? AND Type='ProjectClosed' ORDER BY RecipientID", Long.class, id));
        mvc.perform(post("/api/projects/" + id + "/close").session(s)).andExpect(status().isConflict());     // twice
        mvc.perform(get("/api/projects/" + id).session(s)).andExpect(jsonPath("$.latestTest.TestDate").value(test.toString())); // UC03 shows Done test
    }
}
