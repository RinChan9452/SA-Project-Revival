package com.projectsa.uc06;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.projectsa.common.Check;

/** UC06 Step 5 (5.1–5.3): <<Controller>> Requirement DetectBlank. js/uc06.js runs the same rules first. */
public final class RequirementDetectBlank {

    public record RequirementForm(String title, String detail, String priority, String dueDate, String relatedFeature) {
    }

    static final Set<String> PRIORITIES = Set.of("Low", "Medium", "High");

    private RequirementDetectBlank() {
    }

    public static LocalDate check(RequirementForm f, LocalDate today, Map<String, String> errors) {
        Check c = new Check()
                .required("title", f.title(), "กรุณากรอกหัวข้อ Requirement")                                        // 5.1
                .required("detail", f.detail(), "กรุณากรอกรายละเอียดความต้องการ")
                .rule(f.priority() != null && PRIORITIES.contains(f.priority()), "priority", "กรุณาเลือกระดับความสำคัญ"); // 5.2
        LocalDate due = c.date("dueDate", f.dueDate(), "กรุณากรอกวันที่ลูกค้าต้องการให้เสร็จ");
        if (due != null) {
            c.rule(!due.isBefore(today), "dueDate", "วันที่ต้องการให้เสร็จต้องไม่เป็นวันที่ผ่านมาแล้ว");                // 5.3
        }
        errors.putAll(c.errors());
        return due;
    }
}
