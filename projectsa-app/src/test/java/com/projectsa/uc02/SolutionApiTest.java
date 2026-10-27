package com.projectsa.uc02;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;
import com.projectsa.uc01.ProjectDetectBlank.ProjectForm;
import com.projectsa.uc01.ProjectService;

/** CLAUDE.md §6.2 UC02 Store Project Solution Details. */
@SpringBootTest
@AutoConfigureMockMvc
class SolutionApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired JdbcTemplate jdbc;

    private static MockMultipartFile pdf(String field, String name) {
        return new MockMultipartFile(field, name, "application/pdf", "%PDF-1.4 demo".getBytes());
    }

    private MockMultipartHttpServletRequestBuilder save(long id, Map<String, String> fields, MockHttpSession s) {
        MockMultipartHttpServletRequestBuilder b = multipart("/api/projects/" + id + "/solution");
        fields.forEach(b::param);
        b.session(s);
        return b;
    }

    @Test
    void storeSolution() throws Exception {
        long sale = TestData.register(auth, "u2_sale", Auth.SALE);
        long pre = TestData.register(auth, "u2_pre", Auth.PRESALE);
        TestData.register(auth, "u2_pre2", Auth.PRESALE);
        long pm = TestData.register(auth, "u2_pm", Auth.PM);
        long id = uc01.create(new com.projectsa.common.SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC02 Project", null, "UC02 Co", "c@uc02.com", pre, "Site"));
        MockHttpSession s = TestData.login(mvc, "u2_pre");
        MockHttpSession other = TestData.login(mvc, "u2_pre2");

        mvc.perform(get("/api/projects?status=NewProject").session(s)).andExpect(jsonPath("$[0].ProjectID").value(id));
        mvc.perform(get("/api/projects?status=NewProject").session(other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/projects?status=NewProject").session(TestData.login(mvc, "u2_pm"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/solution").session(s))
                .andExpect(jsonPath("$.project.ProjectName").value("UC02 Project"))
                .andExpect(jsonPath("$.documents.length()").value(0));
        mvc.perform(get("/api/projects/" + id + "/solution").session(other)).andExpect(status().isNotFound());

        Map<String, String> ok = Map.of("startDate", "2026-11-01", "endDate", "2026-12-31", "objective", "Upgrade core",
                "estimatedBudget", "1,500,000", "solutionDetail", "Cisco 9500");
        // Alt 5a + 7.3
        mvc.perform(save(id, Map.of("startDate", "2026-12-31", "endDate", "2026-11-01", "estimatedBudget", "-5"), s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.endDate").value("End Date ต้องไม่ก่อน Start Date"))
                .andExpect(jsonPath("$.fields.objective").exists())
                .andExpect(jsonPath("$.fields.estimatedBudget").exists())
                .andExpect(jsonPath("$.fields.files").value("ต้องมีเอกสาร Solution อย่างน้อย 1 ไฟล์"));
        // Alt 7a: not a PDF
        mvc.perform(save(id, ok, s).file(new MockMultipartFile("solutionFiles", "solution.docx", "x", "x".getBytes())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.files").exists());
        // other presale cannot save
        mvc.perform(save(id, ok, other).file(pdf("solutionFiles", "s.pdf"))).andExpect(status().isConflict());

        mvc.perform(save(id, ok, s).file(pdf("solutionFiles", "solution.pdf")).file(pdf("requirementFiles", "req.pdf")))
                .andExpect(status().isOk());
        assertEquals("WaitingForAssignEngineer", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        assertEquals(0, new java.math.BigDecimal("1500000.00").compareTo(
                jdbc.queryForObject("SELECT EstimatedBudget FROM PROJECT WHERE ProjectID=?", java.math.BigDecimal.class, id)));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_DOCUMENT WHERE ProjectID=?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM PROJECT_DOCUMENT WHERE ProjectID=? AND DocType='ProductRequirement'", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM NOTIFICATION WHERE ProjectID=? AND RecipientID=? AND Type='WaitingAssign'", Integer.class, id, pm));
        // no longer NewProject → saving again is refused
        mvc.perform(save(id, ok, s).file(pdf("solutionFiles", "again.pdf"))).andExpect(status().isConflict());
    }
}
