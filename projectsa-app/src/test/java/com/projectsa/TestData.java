package com.projectsa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.projectsa.auth.AuthService;
import com.projectsa.auth.RegisterDetectBlank.RegisterForm;

/** Test helpers: real F1 Register + real F2 Login (no shortcuts around the design queries). */
public final class TestData {

    public static final String PASSWORD = "secret1";

    private TestData() {
    }

    /** skills = typed skill names (F1 R.5 / R.6 + R.4). */
    public static long register(AuthService auth, String username, String role, String... skills) {
        return auth.register(new RegisterForm(username, role, username + "@t.local", null, username, PASSWORD, role,
                List.of(skills)));
    }

    public static MockHttpSession login(MockMvc mvc, String username) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession();
    }
}
