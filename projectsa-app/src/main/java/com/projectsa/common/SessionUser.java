package com.projectsa.common;

import java.io.Serializable;

/** The logged-in employee kept in the HttpSession: @LoginID and @Role of the design queries. */
public record SessionUser(long id, String role, String firstName, String lastName, String profilePicture)
        implements Serializable {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
