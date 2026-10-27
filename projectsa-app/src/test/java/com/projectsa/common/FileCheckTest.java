package com.projectsa.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** The narrative's file rules: UC02 Step 7, UC06 Step 7, F3 P.5. */
class FileCheckTest {

    private static MockMultipartFile file(String name, int bytes) {
        return new MockMultipartFile("files", name, "application/octet-stream", new byte[bytes]);
    }

    @Test
    void rules() {
        assertNull(FileCheck.SOLUTION.problem(file("solution.PDF", 10)));
        assertNotNull(FileCheck.SOLUTION.problem(file("solution.docx", 10)));
        assertNotNull(FileCheck.SOLUTION.problem(file("empty.pdf", 0)));
        assertNull(FileCheck.REQUIREMENT.problem(file("spec.png", 10)));
        assertNotNull(FileCheck.REQUIREMENT.problem(file("spec.exe", 10)));
        assertNull(FileCheck.PROFILE_PICTURE.problem(file("me.jpg", 10)));
        assertNotNull(FileCheck.PROFILE_PICTURE.problem(file("me.pdf", 10)));
        assertNotNull(FileCheck.PROFILE_PICTURE.problem(file("big.png", 5 * 1024 * 1024 + 1)));
    }
}
