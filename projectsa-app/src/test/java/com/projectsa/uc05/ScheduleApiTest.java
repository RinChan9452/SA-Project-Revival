package com.projectsa.uc05;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.projectsa.uc04.AssignDetectBlank.AssignForm;
import com.projectsa.uc04.AssignService;

/** CLAUDE.md §6.5 UC05 Set Project Test Calendar Notification. */
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired AssignService uc04;
    @Autowired JdbcTemplate jdbc;

    private ResultActions save(long id, String json, MockHttpSession s) throws Exception {
        return mvc.perform(post("/api/projects/" + id + "/schedules").session(s).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void setTestCalendar() throws Exception {
        long sale = TestData.register(auth, "u5_sale", Auth.SALE);
        long pre = TestData.register(auth, "u5_pre", Auth.PRESALE);
        long pm = TestData.register(auth, "u5_pm", Auth.PM);
        long t1 = TestData.register(auth, "u5_t1", Auth.TECH, "CCIE");
        long t2 = TestData.register(auth, "u5_t2", Auth.TECH, "CCNA");
        TestData.register(auth, "u5_t3", Auth.TECH, "Java");
        long id = uc01.create(new SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC05 Project", null, "UC05 Co", "c@uc05.com", pre, "Rayong plant"));
        uc02.save(new SessionUser(pre, Auth.PRESALE, "a", "b", null), id, new SolutionForm("2026-11-01", "2026-12-01", "O", "10", null),
                List.of(new MockMultipartFile("f", "s.pdf", "application/pdf", "x".getBytes())), List.of());
        uc04.assign(new SessionUser(pm, Auth.PM, "a", "b", null), id, new AssignForm(List.of(t1, t2)));
        MockHttpSession s = TestData.login(mvc, "u5_t1");
        MockHttpSession outsider = TestData.login(mvc, "u5_t3");

        mvc.perform(get("/api/projects?assigned=me").session(s)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")].Status").value("Working"));
        mvc.perform(get("/api/projects?assigned=me").session(outsider)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/projects/" + id + "/schedule-form").session(outsider)).andExpect(status().isConflict());
        mvc.perform(get("/api/projects/" + id + "/schedule-form").session(TestData.login(mvc, "u5_sale"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/schedule-form").session(s))
                .andExpect(jsonPath("$.project.ProjectSite").value("Rayong plant"))
                .andExpect(jsonPath("$.project.CustomerEmail").value("c@uc05.com"))
                .andExpect(jsonPath("$.team.length()").value(2));

        // Alt 5a: 5.1–5.4
        save(id, """
                {"testDate":"%s","startTime":"12:00","endTime":"10:00","location":" ","remindBefore":null}"""
                .formatted(LocalDate.now().minusDays(1)), s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.testDate").value("วันที่ทดสอบต้องไม่เป็นวันที่ผ่านมาแล้ว"))
                .andExpect(jsonPath("$.fields.endTime").value("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม"))
                .andExpect(jsonPath("$.fields.location").exists())
                .andExpect(jsonPath("$.fields.remindBefore").exists());

        LocalDate d1 = LocalDate.now().plusDays(20);
        save(id, """
                {"testDate":"%s","startTime":"10:00","endTime":"12:00","location":"Rayong plant","testDetail":"UAT","remindBefore":1440}"""
                .formatted(d1), s).andExpect(status().isCreated());
        assertEquals("Testing", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATION WHERE ProjectID=? AND Type='TestSchedule'", Integer.class, id));
        assertEquals("นัดทดสอบ " + d1 + " 10:00 ที่ Rayong plant", jdbc.queryForObject(
                "SELECT TOP 1 Message FROM NOTIFICATION WHERE ProjectID=? AND Type='TestSchedule'", String.class, id));
        assertEquals(d1.minusDays(1).atTime(10, 0), jdbc.queryForObject(
                "SELECT TOP 1 SendAt FROM NOTIFICATION WHERE ProjectID=? AND Type='TestReminder'", LocalDateTime.class, id));

        // re-test after a fix: Testing → Testing, old appointment Cancelled
        save(id, """
                {"testDate":"%s","startTime":"09:00","endTime":"11:00","location":"Rayong plant","remindBefore":60}"""
                .formatted(d1.plusDays(7)), s).andExpect(status().isCreated());
        assertEquals(List.of("Cancelled", "Scheduled"), jdbc.queryForList(
                "SELECT Status FROM TEST_SCHEDULE WHERE ProjectID=? ORDER BY ScheduleID", String.class, id));
        assertEquals("Testing", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        save(id, "{}", outsider).andExpect(status().isBadRequest());
        save(id, """
                {"testDate":"%s","startTime":"09:00","endTime":"11:00","location":"x","remindBefore":60}""".formatted(d1), outsider)
                .andExpect(status().isConflict());
    }
}
