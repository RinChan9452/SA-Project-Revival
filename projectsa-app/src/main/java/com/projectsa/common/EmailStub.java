package com.projectsa.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** The demo sends no email (CLAUDE.md §4): UC05 Step 8 (customer) and UC06 Step 10 (Sale) only log. */
@Component
public class EmailStub {

    private static final Logger log = LoggerFactory.getLogger(EmailStub.class);

    public void send(String to, String subject) {
        log.info("[EMAIL SKIPPED] to={} subject={}", to, subject);
    }
}
