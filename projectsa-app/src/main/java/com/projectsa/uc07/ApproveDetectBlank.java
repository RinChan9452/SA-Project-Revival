package com.projectsa.uc07;

import java.util.Map;

import com.projectsa.common.Check;

/** UC07 Step 8: <<Controller>> Approve DetectBlank – a decision, and a reason when rejecting (Alt 8a). */
public final class ApproveDetectBlank {

    public record DecisionForm(Long projectId, String decision, String rejectReason) {
    }

    private ApproveDetectBlank() {
    }

    public static Map<String, String> check(DecisionForm f) {
        Check c = new Check()
                .rule(f.projectId() != null, "projectId", "ไม่พบ Project")
                .rule("Approved".equals(f.decision()) || "Rejected".equals(f.decision()), "decision", "กรุณาเลือกอนุมัติ หรือ ปฏิเสธ");
        if ("Rejected".equals(f.decision())) {
            c.required("rejectReason", f.rejectReason(), "กรุณาระบุเหตุผลที่ปฏิเสธ");
        }
        return c.errors();
    }
}
