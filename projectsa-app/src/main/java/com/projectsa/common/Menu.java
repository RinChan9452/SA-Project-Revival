package com.projectsa.common;

import java.util.List;
import java.util.Map;

/** The use cases each role may open (CLAUDE.md §1 table). The header menu is built from this. */
public final class Menu {

    public record Item(String uc, String title, String page) {
    }

    private static final Item UC01 = new Item("UC01", "สร้าง Project Profile", "uc01.html");
    private static final Item UC02 = new Item("UC02", "บันทึก Solution", "uc02.html");
    private static final Item UC03 = new Item("UC03", "ดูรายละเอียด Project", "uc03.html");
    private static final Item UC04 = new Item("UC04", "มอบหมาย Engineer", "uc04.html");
    private static final Item UC05 = new Item("UC05", "ตั้งนัดทดสอบ", "uc05.html");
    private static final Item UC06 = new Item("UC06", "เพิ่ม Requirement ใหม่", "uc06.html");
    private static final Item UC07 = new Item("UC07", "อนุมัติ Requirement", "uc07.html");
    private static final Item UC08 = new Item("UC08", "ปิด Project", "uc08.html");

    private static final Map<String, List<Item>> BY_ROLE = Map.of(
            Auth.SALE, List.of(UC01, UC03, UC07),
            Auth.PRESALE, List.of(UC02, UC03),
            Auth.PM, List.of(UC03, UC04, UC06, UC08),
            Auth.TECH, List.of(UC03, UC05));

    public static final Map<String, String> ROLE_NAMES = Map.of(
            Auth.SALE, "Sale", Auth.PRESALE, "Presale Engineer", Auth.PM, "PM", Auth.TECH, "Tech Engineer");

    private Menu() {
    }

    public static List<Item> forRole(String role) {
        return BY_ROLE.getOrDefault(role, List.of());
    }
}
