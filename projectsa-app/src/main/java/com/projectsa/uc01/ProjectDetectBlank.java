package com.projectsa.uc01;

import java.util.Map;

import com.projectsa.common.Check;

/** UC01 Step 4 (4.1–4.5): <<Controller>> Project DetectBlank. js/uc01.js runs the same rules first. */
public final class ProjectDetectBlank {

    /** customerId = existing customer; or newCustomerName / newCustomerEmail for a new one (Alt: Q5.1). */
    public record ProjectForm(String projectName, Long customerId, String newCustomerName, String newCustomerEmail,
                              Long presaleId, String projectSite) {
        public boolean newCustomer() {
            return customerId == null;
        }
    }

    private ProjectDetectBlank() {
    }

    public static Map<String, String> check(ProjectForm f) {
        Check c = new Check().required("projectName", f.projectName(), "กรุณากรอก Project Name");          // 4.1
        if (f.newCustomer()) {
            c.required("newCustomerName", f.newCustomerName(), "กรุณากรอก Customer Name")                    // 4.2
             .required("newCustomerEmail", f.newCustomerEmail(), "กรุณากรอก Customer Email")                 // 4.3
             .email("newCustomerEmail", f.newCustomerEmail());
        }
        c.rule(f.presaleId() != null, "presaleId", "กรุณาเลือก Presale Engineer in Charge")                   // 4.4
         .required("projectSite", f.projectSite(), "กรุณากรอก Project Site");                               // 4.5
        return c.errors();
    }
}
