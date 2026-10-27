package com.projectsa.uc02;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.projectsa.common.Check;

/** UC02 Step 5 (5.1–5.3): <<Controller>> Solution DetectBlank. js/uc02.js runs the same rules first. */
public final class SolutionDetectBlank {

    public record SolutionForm(String startDate, String endDate, String objective, String estimatedBudget,
                               String solutionDetail) {
    }

    public record Valid(LocalDate start, LocalDate end, BigDecimal budget) {
    }

    private SolutionDetectBlank() {
    }

    public static Valid check(SolutionForm f, Map<String, String> errors) {
        Check c = new Check();
        LocalDate start = c.date("startDate", f.startDate(), "กรุณากรอก Start Date");                         // 5.1
        LocalDate end = c.date("endDate", f.endDate(), "กรุณากรอก End Date");
        if (start != null && end != null) {
            c.rule(!end.isBefore(start), "endDate", "End Date ต้องไม่ก่อน Start Date");
        }
        c.required("objective", f.objective(), "กรุณากรอก Project Objective");                                 // 5.2
        BigDecimal budget = null;                                                                              // 5.3
        if (Check.blank(f.estimatedBudget())) {
            c.rule(false, "estimatedBudget", "กรุณากรอก Estimated Budget");
        } else {
            try {
                budget = new BigDecimal(f.estimatedBudget().strip().replace(",", ""));
                c.rule(budget.signum() > 0, "estimatedBudget", "Estimated Budget ต้องเป็นตัวเลขที่มากกว่า 0");
            } catch (NumberFormatException e) {
                c.rule(false, "estimatedBudget", "Estimated Budget ต้องเป็นตัวเลขที่มากกว่า 0");
            }
        }
        errors.putAll(c.errors());
        return new Valid(start, end, budget);
    }
}
