package com.projectsa.uc07;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.projectsa.uc05.ScheduleDetectBlank.ScheduleForm;
import com.projectsa.uc05.ScheduleService;
import com.projectsa.uc06.RequirementDetectBlank.RequirementForm;
import com.projectsa.uc06.RequirementService;

/** CLAUDE.md §6.7 UC07 Approve New Project Requirement (two rounds). */
@SpringBootTest
@AutoConfigureMockMvc
class ApproveApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired AssignService uc04;
    @Autowired ScheduleService uc05;
    @Autowired RequirementService uc06;
    @Autowired JdbcTemplate jdbc;

    private ResultActions decide(long req, String json, MockHttpSession s) throws Exception {
        return mvc.perform(put("/api/requirements/" + req + "/decision").session(s).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static void tick() throws InterruptedException {
        Thread.sleep(20);           // CreatedDate ordering for the UC07 "round" (Query 11.2)
    }

    @Test
    void approveTwoRounds() throws Exception {
        long sale = TestData.register(auth, "u7_sale", Auth.SALE);
        TestData.register(auth, "u7_sale2", Auth.SALE);
        long pre = TestData.register(auth, "u7_pre", Auth.PRESALE);
        long pm = TestData.register(auth, "u7_pm", Auth.PM);
        long t1 = TestData.register(auth, "u7_t1", Auth.TECH, "CCIE");
        SessionUser saleU = new SessionUser(sale, Auth.SALE, "a", "b", null), preU = new SessionUser(pre, Auth.PRESALE, "a", "b", null),
                pmU = new SessionUser(pm, Auth.PM, "a", "b", null), techU = new SessionUser(t1, Auth.TECH, "a", "b", null);
        long id = uc01.create(saleU, new ProjectForm("UC07 Project", null, "UC07 Co", "c@uc07.com", pre, "Site"));
        uc02.save(preU, id, new SolutionForm("2026-11-01", "2026-12-01", "O", "10", "S"),
                List.of(new MockMultipartFile("f", "s.pdf", "application/pdf", "x".getBytes())), List.of());
        uc04.assign(pmU, id, new AssignForm(List.of(t1)));
        uc05.save(techU, id, new ScheduleForm(LocalDate.now().plusDays(5).toString(), "10:00", "12:00", "Site", null, 60));
        tick();
        String due = LocalDate.now().plusDays(20).toString();
        long vpn = uc06.submit(pmU, id, new RequirementForm("Add VPN", "d", "High", due, null),
                List.of(new MockMultipartFile("attachments", "vpn.pdf", "application/pdf", "V".getBytes())));
        long wifi = uc06.submit(pmU, id, new RequirementForm("Add WiFi", "d", "Low", due, null), List.of());
        MockHttpSession s = TestData.login(mvc, "u7_sale");
        MockHttpSession other = TestData.login(mvc, "u7_sale2");

        mvc.perform(get("/api/projects?status=WaitingForApprove").session(s))
                .andExpect(jsonPath("$[?(@.ProjectID == " + id + ")].PendingCount").value(2));
        mvc.perform(get("/api/projects?status=WaitingForApprove").session(TestData.login(mvc, "u7_pm"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/requirements?status=Pending").session(other)).andExpect(status().isConflict());
        mvc.perform(get("/api/projects/" + id + "/requirements?status=Pending").session(s)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/requirements/" + vpn + "?project=" + id).session(s))
                .andExpect(jsonPath("$.requirement.Title").value("Add VPN"))
                .andExpect(jsonPath("$.attachments[0].FileName").value("vpn.pdf"));

        // Alt 8a
        decide(wifi, "{\"projectId\":" + id + ",\"decision\":\"Rejected\",\"rejectReason\":\" \"}", s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.rejectReason").exists());
        decide(wifi, "{\"projectId\":" + id + ",\"decision\":\"Maybe\"}", s).andExpect(jsonPath("$.fields.decision").exists());

        // round 1: approve VPN → still waiting (Alt 11a); reject WiFi → NewProject
        decide(vpn, "{\"projectId\":" + id + ",\"decision\":\"Approved\"}", s).andExpect(jsonPath("$.ProjectStatus").value("WaitingForApprove"));
        decide(vpn, "{\"projectId\":" + id + ",\"decision\":\"Rejected\",\"rejectReason\":\"x\"}", s).andExpect(status().isConflict()); // Alt 5a
        mvc.perform(get("/api/requirements/" + vpn + "?project=" + id).session(s)).andExpect(status().isConflict());
        decide(wifi, "{\"projectId\":" + id + ",\"decision\":\"Rejected\",\"rejectReason\":\"Out of budget\"}", s)
                .andExpect(jsonPath("$.ProjectStatus").value("NewProject"));
        assertEquals("NewProject", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        assertEquals(List.of("Requirement \"Add VPN\" : Approved", "Requirement \"Add WiFi\" : Rejected (เหตุผล: Out of budget)"),
                jdbc.queryForList("SELECT Message FROM NOTIFICATION WHERE ProjectID=? AND RecipientID=? AND Type='RequirementResult' "
                        + "ORDER BY NotificationID", String.class, id, pm));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM NOTIFICATION WHERE ProjectID=? AND RecipientID=? AND Type='RequirementResult'", Integer.class, id, pre));
        assertEquals(sale, jdbc.queryForObject("SELECT ReviewedByID FROM CUSTOMER_REQUIREMENT WHERE RequirementID=?", Long.class, wifi));

        // round 2: redesign → reassign → retest → one more requirement rejected → Finish (round-1 approval not counted)
        uc02.save(preU, id, new SolutionForm("2026-11-01", "2026-12-15", "O2", "20", "S2"), List.of(), List.of());
        uc04.assign(pmU, id, new AssignForm(List.of(t1)));
        tick();
        uc05.save(techU, id, new ScheduleForm(LocalDate.now().plusDays(9).toString(), "10:00", "12:00", "Site", null, 60));
        tick();
        long mon = uc06.submit(pmU, id, new RequirementForm("Add Monitoring", "d", "Medium", due, null), List.of());
        decide(mon, "{\"projectId\":" + id + ",\"decision\":\"Rejected\",\"rejectReason\":\"Not in scope\"}", s)
                .andExpect(jsonPath("$.ProjectStatus").value("Finish"));
        assertEquals("Finish", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
    }
}
