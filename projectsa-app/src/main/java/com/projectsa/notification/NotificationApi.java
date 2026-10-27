package com.projectsa.notification;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;
import com.projectsa.common.Sql;

import jakarta.servlet.http.HttpSession;

/**
 * F4 Notification Bell (Query N.1–N.4). Every role. Only notifications whose SendAt has passed are shown,
 * so the UC05 reminders appear at the right time without a background job.
 */
@RestController
public class NotificationApi {

    private final Sql sql;

    public NotificationApi(Sql sql) {
        this.sql = sql;
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Object> unread(HttpSession session) {
        return Map.of("Unread", sql.count("F4", "N.1", Auth.params(Auth.current(session))));       // N.1
    }

    @GetMapping("/api/notifications")
    public List<Map<String, Object>> latest(HttpSession session) {
        return sql.rows("F4", "N.2", Auth.params(Auth.current(session)));                           // N.2
    }

    @PutMapping("/api/notifications/{id}/read")
    public Map<String, Object> read(@PathVariable long id, HttpSession session) {
        Map<String, Object> p = Auth.params(Auth.current(session));
        p.put("NotificationID", id);
        return Map.of("updated", sql.update("F4", "N.3", p));                                        // N.3
    }

    @PutMapping("/api/notifications/read-all")
    public Map<String, Object> readAll(HttpSession session) {
        return Map.of("updated", sql.update("F4", "N.4", Auth.params(Auth.current(session))));     // N.4
    }
}
