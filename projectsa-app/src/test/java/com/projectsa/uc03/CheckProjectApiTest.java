package com.projectsa.uc03;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
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

/** CLAUDE.md §6.3 UC03 Check Project Details. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckProjectApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired JdbcTemplate jdbc;

    @Test
    void checkProjectDetails() throws Exception {
        long sale = TestData.register(auth, "u3_sale", Auth.SALE);
        long pre = TestData.register(auth, "u3_pre", Auth.PRESALE);
        TestData.register(auth, "u3_pm", Auth.PM);
        TestData.register(auth, "u3_tech", Auth.TECH, "CCIE");
        long id = uc01.create(new SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC03 Zebra", null, "UC03 Co", "c@uc03.com", pre, "Khon Kaen"));
        MockHttpSession saleS = TestData.login(mvc, "u3_sale");
        MockHttpSession techS = TestData.login(mvc, "u3_tech");
        MockHttpSession pmS = TestData.login(mvc, "u3_pm");

        // 1.1 by role + keyword
        mvc.perform(get("/api/projects").session(saleS)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").exists());
        mvc.perform(get("/api/projects").session(pmS)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").exists());
        mvc.perform(get("/api/projects").session(techS)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/projects?keyword=Zebra").session(pmS)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/projects?keyword=" + id).session(pmS)).andExpect(jsonPath("$[0].ProjectName").value("UC03 Zebra"));
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());

        // Alt 3a + Alt 4a (no solution / PM / engineers / test yet)
        mvc.perform(get("/api/projects/" + id).session(techS)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ไม่พบ Project"));
        mvc.perform(get("/api/projects/999999").session(pmS)).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + id).session(saleS))
                .andExpect(jsonPath("$.project.SaleInCharge").value("u3_sale Sale"))
                .andExpect(jsonPath("$.project.PMName").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.latestTest").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.engineers.length()").value(0))
                .andExpect(jsonPath("$.documents.length()").value(0));

        // with a stored solution PDF → 4.4 / 4.7 filled, 6.1 download
        uc02.save(new SessionUser(pre, Auth.PRESALE, "a", "b", null), id,
                new SolutionForm("2026-11-01", "2026-12-01", "Objective", "1000", "Detail"),
                List.of(new MockMultipartFile("solutionFiles", "แผนงาน.pdf", "application/pdf", "PDF-DATA".getBytes())), List.of());
        long doc = jdbc.queryForObject("SELECT DocumentID FROM PROJECT_DOCUMENT WHERE ProjectID = ?", Long.class, id);
        int notes = jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATION", Integer.class);
        mvc.perform(get("/api/projects/" + id).session(saleS))
                .andExpect(jsonPath("$.project.Objective").value("Objective"))
                .andExpect(jsonPath("$.documents[0].FileName").value("แผนงาน.pdf"));
        mvc.perform(get("/api/projects/" + id + "/documents/" + doc).session(saleS)).andExpect(status().isOk())
                .andExpect(content().bytes("PDF-DATA".getBytes()))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")));
        mvc.perform(get("/api/projects/" + id + "/documents/" + doc).session(techS)).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/" + id + "/requirements/1/attachment?file=x").session(saleS)).andExpect(status().isNotFound());
        // read only: UC03 wrote nothing
        assertEquals(notes, jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATION", Integer.class));
    }
}
