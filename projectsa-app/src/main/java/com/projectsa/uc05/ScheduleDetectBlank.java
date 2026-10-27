package com.projectsa.uc05;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import com.projectsa.common.Check;

/** UC05 Step 5 (5.1–5.4): <<Controller>> Schedule DetectBlank. js/uc05.js runs the same rules first. */
public final class ScheduleDetectBlank {

    public record ScheduleForm(String testDate, String startTime, String endTime, String location, String testDetail,
                               Integer remindBefore) {
    }

    public record Valid(LocalDate date, LocalTime start, LocalTime end) {
    }

    private ScheduleDetectBlank() {
    }

    public static Valid check(ScheduleForm f, LocalDate today, Map<String, String> errors) {
        Check c = new Check();
        LocalDate date = c.date("testDate", f.testDate(), "กรุณากรอกวันที่ทดสอบ");
        if (date != null) {
            c.rule(!date.isBefore(today), "testDate", "วันที่ทดสอบต้องไม่เป็นวันที่ผ่านมาแล้ว");                     // 5.1
        }
        LocalTime start = c.time("startTime", f.startTime(), "กรุณากรอกเวลาเริ่ม");
        LocalTime end = c.time("endTime", f.endTime(), "กรุณากรอกเวลาสิ้นสุด");
        if (start != null && end != null) {
            c.rule(end.isAfter(start), "endTime", "เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม");                                // 5.2
        }
        c.required("location", f.location(), "กรุณากรอกสถานที่ทดสอบ");                                           // 5.3
        c.rule(f.remindBefore() != null && f.remindBefore() > 0, "remindBefore", "กรุณาเลือกเวลาแจ้งเตือนล่วงหน้า");  // 5.4
        errors.putAll(c.errors());
        return new Valid(date, start, end);
    }
}
