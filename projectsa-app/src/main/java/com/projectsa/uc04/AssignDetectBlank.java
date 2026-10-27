package com.projectsa.uc04;

import java.util.List;
import java.util.Map;

import com.projectsa.common.Check;

/** UC04 Step 7: <<Controller>> Assign DetectBlank – at least 1 Tech Engineer selected (Alt 7a). */
public final class AssignDetectBlank {

    public record AssignForm(List<Long> engineerIds) {
    }

    private AssignDetectBlank() {
    }

    public static Map<String, String> check(AssignForm f) {
        return new Check()
                .rule(f.engineerIds() != null && !f.engineerIds().isEmpty(), "engineerIds", "กรุณาเลือก Tech Engineer อย่างน้อย 1 คน")
                .errors();
    }
}
