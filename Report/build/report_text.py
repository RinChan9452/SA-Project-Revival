# -*- coding: utf-8 -*-
"""Text of Report.docx. [[...]] = placeholder / assumption the team must fill or confirm (shown highlighted).
**...** = bold."""

TITLE_TH = "ระบบบริหารจัดการโครงการ (Project Management Program)"
TITLE_EN = "Project Management Program for an IT-Solution Company"

COVER = {
    "course": "[[รหัสวิชา ชื่อวิชา (เช่น 01xxxxxx การวิเคราะห์และออกแบบระบบ)]]",
    "teacher": "[[ชื่ออาจารย์ผู้สอน]]",
    "term": "[[ภาคเรียนที่ x ปีการศึกษา 25xx]]",
    "faculty": "[[ภาควิชา / คณะ / มหาวิทยาลัย]]",
    "members": 5,
}

ABSTRACT_TH = [
    "รายงานฉบับนี้นำเสนอการวิเคราะห์และออกแบบ **ระบบบริหารจัดการโครงการ (Project Management Program)** สำหรับบริษัทที่จำหน่ายและติดตั้งระบบไอทีให้ลูกค้า "
    "ในระบบเดิม ฝ่าย Sale, Presale Engineer, Project Manager (PM) และ Technical Engineer ส่งต่อข้อมูลโครงการกันทางอีเมลและเอกสารกระดาษ "
    "ทำให้เกิดปัญหา 5 ข้อ ได้แก่ นัดทดสอบกับลูกค้าคลาดเคลื่อน สถานที่ทำงานไม่ตรงกับนัด โครงการล่าช้าเมื่อลูกค้าเพิ่ม Requirement "
    "PM ไม่ทราบความสามารถ (Skill) ของ Engineer และรายละเอียดโครงการตกหล่นเมื่อส่งต่องาน",
    "ระบบใหม่รวมข้อมูลโครงการไว้ที่เดียว ประกอบด้วย 8 Use Case คือ สร้าง Project Profile, บันทึก Solution, ดูรายละเอียด Project, "
    "มอบหมาย Engineer ตาม Skill, ตั้งนัดทดสอบพร้อมแจ้งเตือน, บันทึก Requirement ใหม่, อนุมัติ Requirement ใหม่ และปิด Project "
    "สถานะของโครงการถูกควบคุมด้วย State Diagram และทุกขั้นตอนแจ้งเตือนผู้เกี่ยวข้องผ่านกระดิ่งแจ้งเตือนในระบบ",
    "การออกแบบประกอบด้วย Business Process แบบ As-is และ To-be, Use Case Diagram, CRUD Table, Use Case Description พร้อม Query, "
    "ER Diagram แบบ Peter Chen และ Logical Schema (11 ตาราง), Sequence / Collaboration / Class / State Diagram "
    "โปรแกรมพัฒนาด้วย Java 21, Spring Boot 3 และฐานข้อมูล H2 (โหมด SQL Server) โดยทุกคำสั่ง SQL (87 คำสั่ง) ผ่านการทดสอบตลอดวงจรชีวิตของโครงการ",
]
KEYWORDS_TH = "คำสำคัญ: ระบบบริหารจัดการโครงการ, การวิเคราะห์และออกแบบระบบ, UML, Use Case, ER Diagram, Spring Boot"

ABSTRACT_EN = [
    "This report presents the analysis and design of a **Project Management Program** for a company that sells and installs IT systems. "
    "In the current process, Sale, Presale Engineers, the Project Manager (PM) and Technical Engineers pass project information by e-mail and paper. "
    "This causes five problems: customer test appointments get mixed up, the work site does not match the appointment, projects are delayed when "
    "customers add requirements, the PM does not know each engineer's skills, and project details are lost when work is handed over.",
    "The new system keeps one shared project record and provides eight use cases: Create Project Profile, Store Project Solution Details, "
    "Check Project Details, Assign Engineer to Project, Set Project Test Calendar Notification, Add Customer New Requirement, "
    "Approve New Project Requirement and Close Project. The project status follows a state diagram, and every step notifies the people involved "
    "through an in-app notification bell.",
    "The design includes As-is and To-be business processes, use case diagrams, CRUD tables, use case descriptions with queries, a Peter Chen "
    "ER diagram with its logical schema (11 tables), and sequence, collaboration, class and state diagrams. The program is built with Java 21, "
    "Spring Boot 3 and an H2 database in SQL Server mode; all 87 SQL statements were tested through the whole project life cycle.",
]
KEYWORDS_EN = "Keywords: project management, system analysis and design, UML, use case, ER diagram, Spring Boot"

INTRO = [
    "[[ชื่อบริษัท]] เป็นบริษัทผู้ให้บริการ IT Solution ที่จำหน่าย ออกแบบ และติดตั้งระบบเครือข่าย ระบบ Server และระบบสารสนเทศให้แก่ลูกค้าองค์กร "
    "ก่อตั้งเมื่อ [[ปี พ.ศ.]] มีพนักงานประมาณ [[จำนวน]] คน แบ่งเป็นฝ่ายขาย (Sale) ฝ่าย Presale Engineer ผู้จัดการโครงการ (PM) "
    "ทีม Technical Engineer และฝ่ายจัดซื้อ / Stock โดยในแต่ละปีบริษัทดูแลโครงการประมาณ [[จำนวน]] โครงการ",
    "การดำเนินโครงการหนึ่งโครงการต้องส่งต่อข้อมูลระหว่างหลายฝ่าย ตั้งแต่ Sale ได้รับโครงการ Presale ออกแบบ Solution PM วางแผนและจัดทีม "
    "จนถึง Engineer ติดตั้งและทดสอบระบบกับลูกค้า ปัจจุบันการส่งต่อข้อมูลทั้งหมดทำผ่านอีเมลและเอกสารกระดาษ ข้อมูลของโครงการจึงกระจายอยู่หลายที่ "
    "เมื่อมีการเปลี่ยนแปลง เช่น ลูกค้าเพิ่ม Requirement หรือเลื่อนนัดทดสอบ ข้อมูลที่แต่ละฝ่ายถืออยู่จึงไม่ตรงกัน",
    "คณะผู้จัดทำจึงวิเคราะห์และออกแบบ Project Management Program เพื่อเป็นแหล่งข้อมูลกลางของโครงการ ให้ทุกฝ่ายเห็นข้อมูลชุดเดียวกัน "
    "ควบคุมสถานะของโครงการ และแจ้งเตือนผู้รับผิดชอบโดยอัตโนมัติ ส่วนงานจัดซื้ออุปกรณ์ การติดตั้งที่ Site และการทำเอกสาร UAT / Handover "
    "ยังคงทำแบบ manual ตามขอบเขตของโครงงาน",
]

PROBLEMS_INTRO = ("จากการสัมภาษณ์ผู้ปฏิบัติงานของ [[ชื่อบริษัท]] พบปัญหาที่ Application จะเข้าไปแก้ไข ดังนี้ "
                  "(ตัวเลขที่ไฮไลต์เป็นค่าประมาณการ ให้แทนด้วยข้อมูลจริงที่เก็บจากหน่วยงาน)")
PROBLEMS = [
    ("นัดทดสอบระบบกับลูกค้าคลาดเคลื่อน",
     "นัดหมายถูกแจ้งทางอีเมล / โทรศัพท์ จึงมีการเข้าใจวัน เวลาไม่ตรงกันประมาณ [[2 ครั้งต่อเดือน]] แต่ละครั้งทีม [[3 คน]] เสียเวลารอหรือเดินทางเปล่า [[4 ชั่วโมง]]"),
    ("สถานที่ทำงานไม่ตรงกับนัด",
     "ที่อยู่ Site ถูกส่งต่อหลายทอด ทำให้ Engineer ไปผิดอาคาร / ผิดสาขาประมาณ [[1 ครั้งต่อเดือน]] เสียเวลาเดินทางซ้ำ [[3 ชั่วโมง]] และค่าเดินทาง [[1,500 บาท]] ต่อครั้ง"),
    ("โครงการล่าช้าเมื่อลูกค้าเพิ่ม Requirement",
     "Requirement ใหม่ถูกส่งต่อทางอีเมลไปให้ Sale พิจารณา ใช้เวลารอการอนุมัติเฉลี่ย [[5 วันทำการ]] ต่อครั้ง และเกิดประมาณ [[2 ครั้งต่อโครงการ]]"),
    ("PM ไม่รู้ความสามารถของ Engineer",
     "PM จัดคนเข้าโครงการจากความจำ ทำให้ต้องเปลี่ยนตัว Engineer กลางโครงการประมาณ [[1 ใน 4 โครงการ]] เสียเวลาส่งมอบงานใหม่ [[2 วัน]] ต่อครั้ง"),
    ("รายละเอียดโครงการตกหล่นเมื่อส่งต่องาน",
     "แต่ละฝ่ายต้องค้นหาเอกสาร Solution / Requirement จากอีเมลเก่า เฉลี่ย [[30 นาที]] ต่อครั้ง ประมาณ [[10 ครั้งต่อสัปดาห์]]"),
]

ASIS_DESC = [
    ("Sale", "เริ่มจาก Sale **จัดหา Project (1)** แล้วตัดสินใจร่วมกับ Presale ว่าจะรับโครงการหรือไม่ ถ้าไม่รับจะกลับไปจัดหาโครงการใหม่ "
             "เมื่อ Presale ออกแบบ Solution แล้ว Sale นำไปให้ลูกค้า Confirm ถ้าลูกค้าไม่ Confirm จะกลับไปที่ขั้นตอนที่ 1 "
             "ในช่วงเตรียมอุปกรณ์ Sale **แจ้งจัดซื้อ / Stock (9)** **รอรับแจ้งของครบจากจัดซื้อ / Stock (12)** แล้ว **แจ้ง Engineer และ PM ว่าของครบแล้ว (13)** "
             "ผลลัพธ์คือโครงการที่ลูกค้ายืนยันแล้ว และอุปกรณ์พร้อมติดตั้ง"),
    ("Presale Engineer", "Presale **ออกแบบ Solution และวางแผนการทำ Project (2)** เมื่อลูกค้า Confirm แล้วจึง **เริ่ม Project (3)** "
                         "**ส่ง Solution และแผน Project ให้ PM ทางเมล / เอกสาร (4)** และ **แจ้งอุปกรณ์ที่ต้องใช้ภายใน Project กับ Sale (8)** "
                         "ผลลัพธ์คือเอกสาร Solution ที่ส่งต่อทางอีเมล (จุดเกิด Pain Point 5)"),
    ("จัดซื้อ / Stock", "ตรวจว่าของในคลังครบหรือไม่ ถ้าขาดจะ **จัดซื้อสั่งตาม Spec ที่ได้รับแจ้งมา (10)** แล้วตรวจซ้ำ เมื่อครบแล้ว **แจ้ง Sale (11)** "
                        "ผลลัพธ์คืออุปกรณ์ครบตามรายการ"),
    ("PM", "PM **วางแผนงานให้ตรงตาม Solution ที่ Presale วางไว้ (5)** **กำหนดทีม Engineer เข้า Project (6)** (Pain Point 4) "
           "และ **PM และ Engineer วางแผนร่วมกัน (7)** ผลลัพธ์คือแผนงานและทีมของโครงการ"),
    ("Technical Engineer", "**PM และ Engineer ติดตั้งอุปกรณ์ที่ Site (14)** (Pain Point 2) แล้ว **ทดสอบระบบ (15)** ถ้าเกิดปัญหาให้ **Engineer แก้ปัญหาที่เกิด (16)** "
                           "และทดสอบใหม่ เมื่อไม่มีปัญหาจึง **นัดทดสอบระบบกับลูกค้า (17)** (Pain Point 1) ถ้าระบบตรงตามความต้องการ จะ "
                           "**จัดทำเอกสาร UAT และ Handover document (19)** และส่งมอบ Project ถ้าระบบมีปัญหาจะกลับไปแก้ไข (16) "
                           "ถ้าลูกค้าต้องการเพิ่ม Engineer **แจ้งความต้องการของลูกค้าให้ Sale (18)** (Pain Point 3) แล้วกลับไปออกแบบ Solution (2) ผ่านจุดเชื่อม A"),
]

# (pain point, where, calculation lines, measurable target)
ASIS_PROBLEMS = [
    ("1. นัดทดสอบกับลูกค้าคลาดเคลื่อน (Process 17)",
     "[[2 ครั้ง/เดือน × 3 คน × 4 ชม.]] = [[24 ชั่วโมง-คน/เดือน]] หรือ [[288 ชั่วโมง-คน/ปี]]",
     "UC05 บันทึกนัดในระบบและแจ้งเตือนทันที + แจ้งเตือนล่วงหน้า → วัดจำนวนนัดที่คลาดเคลื่อนต่อเดือน (เป้าหมาย 0)"),
    ("2. สถานที่ทำงานไม่ตรงกับนัด (Process 14)",
     "[[1 ครั้ง/เดือน × (3 ชม. × 3 คน + ค่าเดินทาง 1,500 บาท)]] = [[9 ชั่วโมง-คน และ 1,500 บาท/เดือน]] หรือ [[18,000 บาท/ปี]]",
     "UC01 บันทึก Project Site และ UC05 ใช้ Project Site เป็นสถานที่นัดเริ่มต้น → วัดจำนวนครั้งที่ไปผิดสถานที่ (เป้าหมาย 0)"),
    ("3. โครงการล่าช้าเมื่อลูกค้าเพิ่ม Requirement (Process 18)",
     "[[2 ครั้ง/โครงการ × 5 วันทำการ]] = [[10 วันทำการ/โครงการ]]",
     "UC06 บันทึก Requirement และแจ้ง Sale ทันที, UC07 อนุมัติในระบบ → วัดเวลาตั้งแต่บันทึกจนอนุมัติ (เป้าหมาย ≤ 1 วันทำการ)"),
    ("4. PM ไม่รู้ Skill ของ Engineer (Process 6)",
     "[[1 ใน 4 โครงการ × 2 วัน]] = เฉลี่ย [[0.5 วันต่อโครงการ]]",
     "UC04 Filter Engineer ตาม Skill และแสดงจำนวนโครงการที่ทำอยู่ → วัดจำนวนครั้งที่เปลี่ยนตัว Engineer กลางโครงการ"),
    ("5. รายละเอียดโครงการตกหล่นเมื่อส่งต่องาน (Process 4)",
     "[[30 นาที × 10 ครั้ง/สัปดาห์]] = [[5 ชั่วโมง/สัปดาห์]] หรือ [[240 ชั่วโมง/ปี]]",
     "UC02 เก็บ Solution และเอกสาร, UC03 ดูรายละเอียดล่าสุด → วัดเวลาค้นหาข้อมูล (เป้าหมาย < 1 นาที ต่อครั้ง)"),
]

TOBE_DESC = [
    ("Sale", "Sale **จัดหา Project (1)** เมื่อตัดสินใจรับโครงการแล้ว **สร้าง Project Profile (2, UC01)** ในระบบ พร้อม Project Site และ Presale ผู้รับผิดชอบ "
             "ระบบแจ้ง Presale ทันที ต่อมา Sale **แจ้งจัดซื้อ / Stock (12)** และ **แจ้ง PM และ Engineer ว่าอุปกรณ์ครบ (14)** (manual) "
             "เมื่อ PM บันทึก Requirement ใหม่ Sale **Check Project Detail (21, UC03)** แล้ว **อนุมัติ Requirement ใหม่ (22, UC07)** "
             "ถ้าอนุมัติอย่างน้อย 1 ข้อ โครงการกลับไปให้ Presale ปรับ Solution ผ่านจุดเชื่อม A ถ้าไม่อนุมัติทุกข้อ ระบบปิดโครงการ (Finish) ให้ แล้วไปจัดทำเอกสาร UAT"),
    ("Presale Engineer", "Presale ได้รับแจ้งเตือน **Check Project Detail (3, UC03)** **ออกแบบ Solution และวางแผน Project (4)** (manual) "
                         "เมื่อลูกค้า Confirm จึง **เริ่ม Project (5)** แล้ว **บันทึก Solution + เอกสารลงระบบ (6, UC02)** ระบบเปลี่ยนสถานะเป็น "
                         "WaitingForAssignEngineer และแจ้ง PM ทุกคน พร้อมกันนั้น Presale **แจ้งอุปกรณ์ที่ต้องใช้ให้ Sale (11)** (manual)"),
    ("จัดซื้อ / Stock", "ตรวจของในคลัง ถ้าขาดจะ **จัดซื้อสั่งตาม Spec (13)** เมื่อครบจึงแจ้ง Sale (ยังเป็น manual ตามขอบเขต)"),
    ("PM", "PM **Check Project Detail (7, UC03)** เพื่อดู Solution **วางแผนงานให้ตรงตาม Solution (8)** แล้ว **มอบหมาย Engineer ตาม Skill (9, UC04)** "
           "โดย Filter ตาม Skill และเห็นจำนวนโครงการที่แต่ละคนทำอยู่ ระบบเปลี่ยนสถานะเป็น Working และแจ้ง Engineer จากนั้น **PM และ Engineer วางแผนร่วมกัน (10)** "
           "หลังทดสอบกับลูกค้า ถ้ามี Requirement ใหม่ PM **บันทึก Requirement ใหม่ สถานะรออนุมัติ (20, UC06)** ถ้าผ่านตามความต้องการ PM "
           "**ปิด Project + แจ้งเตือนผู้รับผิดชอบทุกคน (23, UC08)**"),
    ("Technical Engineer", "Engineer **Check Project Detail (15, UC03)** **ติดตั้งอุปกรณ์ที่ Site (16)** **ทดสอบระบบภายใน (17)** และ **แก้ปัญหา (18)** (manual) "
                           "เมื่อผ่านแล้ว **ตั้งนัดทดสอบกับลูกค้า + แจ้งเตือน (19, UC05)** ระบบเปลี่ยนสถานะเป็น Testing แจ้ง PM และทีมทันที และแจ้งเตือนล่วงหน้าตามเวลาที่ตั้ง "
                           "สุดท้าย **จัดทำเอกสาร UAT และ Handover (24)** (manual) แล้วส่งมอบ Project"),
]
TOBE_MANUAL = ("Process ที่ยังเป็น manual: 1, 4, 5, 8, 10, 11, 12, 13, 14, 16, 17, 18, 24 (งานจัดหาโครงการ ออกแบบ Solution จัดซื้อ / Stock "
               "วางแผนงาน ติดตั้งและทดสอบภายใน และเอกสาร UAT / Handover) ส่วน Process 2, 3, 6, 7, 9, 15, 19, 20, 21, 22, 23 ทำผ่านระบบ (UC01–UC08)")
TOBE_FIX = [
    ("Pain Point 1 นัดทดสอบคลาดเคลื่อน", "Process 19 (UC05) – นัดถูกบันทึกในระบบ แจ้งเตือนทุกคนทันที และแจ้งเตือนซ้ำก่อนถึงเวลา"),
    ("Pain Point 2 สถานที่ไม่ตรงนัด", "Process 2 (UC01) บันทึก Project Site และ Process 19 (UC05) ใช้ Project Site เป็นสถานที่นัดเริ่มต้น"),
    ("Pain Point 3 Project delay เมื่อเพิ่ม Requirement", "Process 20 (UC06) และ 22 (UC07) – บันทึกและอนุมัติในระบบพร้อมแจ้งเตือน"),
    ("Pain Point 4 PM ไม่รู้ Skill ของ Engineer", "Process 9 (UC04) – Filter Engineer ตาม Skill และจำนวนงานที่รับผิดชอบ"),
    ("Pain Point 5 รายละเอียดหายตอนส่งต่องาน", "Process 6 (UC02) เก็บ Solution / เอกสาร และ Process 3, 7, 15, 21 (UC03) ให้ทุกฝ่ายดูข้อมูลล่าสุด"),
]

BP_UC_ROWS = [  # process, name, lane, UC / manual
    ("1", "จัดหา Project", "Sale", "M1 (manual)"),
    ("2", "สร้าง Project Profile", "Sale", "UC01 Create Project Profile"),
    ("3", "Check Project Detail", "Presale Engineer", "UC03 Check Project Details"),
    ("4", "ออกแบบ Solution และวางแผน Project", "Presale Engineer", "M2 (manual)"),
    ("5", "เริ่ม Project", "Presale Engineer", "M3 (manual)"),
    ("6", "บันทึก Solution + เอกสารลงระบบ", "Presale Engineer", "UC02 Store Project Solution Details"),
    ("7", "Check Project Detail", "PM", "UC03 Check Project Details"),
    ("8", "วางแผนงานให้ตรงตาม Solution", "PM", "M5 (manual)"),
    ("9", "มอบหมาย Engineer ตาม Skill", "PM", "UC04 Assign Engineer to Project"),
    ("10", "PM และ Engineer วางแผนร่วมกัน", "PM", "M5 (manual)"),
    ("11", "แจ้งอุปกรณ์ที่ต้องใช้ให้ Sale", "Presale Engineer", "M3 (manual)"),
    ("12", "แจ้งจัดซื้อ / Stock", "Sale", "M4 (manual)"),
    ("13", "จัดซื้อสั่งตาม Spec", "จัดซื้อ / Stock", "M4 (manual)"),
    ("14", "แจ้ง PM และ Engineer ว่าอุปกรณ์ครบ", "Sale", "M4 (manual)"),
    ("15", "Check Project Detail", "Technical Engineer", "UC03 Check Project Details"),
    ("16", "ติดตั้งอุปกรณ์ที่ Site", "Technical Engineer", "M6 (manual)"),
    ("17", "ทดสอบระบบภายใน", "Technical Engineer", "M6 (manual)"),
    ("18", "Engineer แก้ปัญหา", "Technical Engineer", "M6 (manual)"),
    ("19", "ตั้งนัดทดสอบกับลูกค้า + แจ้งเตือน", "Technical Engineer", "UC05 Set Project Test Calendar Notification"),
    ("20", "บันทึก Requirement ใหม่ สถานะรออนุมัติ", "PM", "UC06 Add Customer New Requirement"),
    ("21", "Check Project Detail", "Sale", "UC03 Check Project Details"),
    ("22", "อนุมัติ Requirement ใหม่?", "Sale", "UC07 Approve New Project Requirement"),
    ("23", "ปิด Project + แจ้งเตือนผู้รับผิดชอบทุกคน", "PM", "UC08 Close Project"),
    ("24", "จัดทำเอกสาร UAT และ Handover", "Technical Engineer", "M7 (manual)"),
]
UC_FROM_BP = [  # UC, name, actor (= swim lane), processes, pain point
    ("UC01", "Create Project Profile", "Sale", "2", "2"),
    ("UC02", "Store Project Solution Details", "Presale Engineer", "6", "5"),
    ("UC03", "Check Project Details", "Sale, Presale Engineer, PM, Technical Engineer", "3, 7, 15, 21", "5"),
    ("UC04", "Assign Engineer to Project", "PM", "9", "4"),
    ("UC05", "Set Project Test Calendar Notification", "Technical Engineer", "19", "1, 2"),
    ("UC06", "Add Customer New Requirement", "PM", "20", "3"),
    ("UC07", "Approve New Project Requirement", "Sale", "22", "3"),
    ("UC08", "Close Project", "PM", "23", "– (ปิดวงจรสถานะ)"),
]

UC_FIRST_DESC = ("Use Case Diagram ฉบับแรกได้จาก Process ที่ใช้คอมพิวเตอร์ใน To-be BP (กล่องสีม่วง) มี Actor 4 กลุ่มตรงกับ Swim lane คือ Sale, "
                 "Presale Engineer, PM และ Technical Engineer (ฝ่ายจัดซื้อ / Stock ไม่มี Use Case เพราะงานจัดซื้อยังเป็น manual) "
                 "และมี Use Case 7 ตัว (UC1–UC7)")

NOUNS = ("Entity ในตาราง CRUD ได้จากการวิเคราะห์คำนามใน Business Process และ Use Case Description ได้แก่ พนักงาน (Sale, Presale, PM, Engineer) → EMPLOYEE, "
         "ความสามารถ / Skill → SKILL และ ENGINEER_SKILL, ลูกค้า → CUSTOMER, โครงการ / Project Profile / Solution → PROJECT, "
         "ทีม Engineer ของโครงการ → PROJECT_ASSIGNMENT, นัดทดสอบ / Calendar → TEST_SCHEDULE, Requirement ใหม่ → CUSTOMER_REQUIREMENT, "
         "เอกสารแนบของ Requirement → REQUIREMENT_ATTACHMENT, เอกสาร Solution / Product Requirement → PROJECT_DOCUMENT และ การแจ้งเตือน → NOTIFICATION")

CRUD_FIRST_ANALYSIS = [
    "เมื่อพิจารณาแต่ละ Entity ว่ามีครบวงจร C (สร้าง) R (อ่าน) U (แก้ไข) หรือไม่ พบว่ายังขาด Use Case ดังนี้",
    "**PROJECT** – มี C (UC01) และ U (UC02, UC04, UC05, UC06, UC07) แต่เมื่อลูกค้าทดสอบผ่านและไม่มี Requirement ใหม่ ไม่มี Use Case ใดเปลี่ยนสถานะจาก Testing เป็น Finish "
    "(สถานะ Finish เกิดได้ทางเดียวคือ UC07 ไม่อนุมัติ Requirement ทั้งหมด) → **ขาด UC08 Close Project** ให้ PM ปิดโครงการ",
    "**TEST_SCHEDULE** – UC05 สร้างและยกเลิกนัด (Cancelled) แต่ไม่มี Use Case ใดบันทึกว่านัดทดสอบครั้งสุดท้ายเสร็จสิ้น (Done) → แก้ด้วย **UC08 Close Project** (Query 7.1)",
    "**NOTIFICATION** – ทุก Use Case สร้างการแจ้งเตือน (C) แต่ไม่มี Use Case ใดอ่าน (R) หรือทำเครื่องหมายว่าอ่านแล้ว (U) → เพิ่มฟังก์ชัน **กระดิ่งแจ้งเตือน (F4)** ที่หัวทุกหน้าจอ",
    "**EMPLOYEE, SKILL, ENGINEER_SKILL** – ถูกอ่าน (R) อย่างเดียว ไม่มีที่สร้าง (C) หรือแก้ไข (U) → เพิ่มฟังก์ชัน **Register (F1)**, **Login (F2)** และ **Profile (F3)** "
    "ซึ่งเป็นฟังก์ชันพื้นฐานของระบบ ไม่ได้มาจาก Process ใน BP จึงไม่แสดงใน Use Case Diagram แต่แสดงในตาราง CRUD ส่วนเสริม",
    "ไม่มี Use Case ใดลบข้อมูล (D) โดยเจตนา ข้อมูลเก่าถูกเก็บไว้ด้วยการเปลี่ยนสถานะแทน เช่น นัดที่ถูกแทนเป็น Cancelled, Requirement ที่ไม่อนุมัติเป็น Rejected",
]
UC_REVISED_DESC = ("Use Case Diagram ฉบับปรับปรุงเพิ่ม **UC8 Close Project** (Actor: PM) ตามผลการวิเคราะห์ CRUD ทำให้ระบบมี 8 Use Case "
                   "และสอดคล้องกับ Process 23 ใน To-be BP")
CRUD_REVISED_DESC = [
    "ตาราง CRUD ฉบับทบทวนเพิ่มแถว UC08 Close Project: อ่าน PROJECT / EMPLOYEE / CUSTOMER / PROJECT_ASSIGNMENT / CUSTOMER_REQUIREMENT, "
    "แก้ไข PROJECT.Status เป็น Finish, แก้ไข TEST_SCHEDULE.Status เป็น Done และสร้าง NOTIFICATION ถึง Sale, Presale และ Engineer ทุกคน",
    "ทุก Entity จึงมีทั้ง C, R และ U ครบ (EMPLOYEE / SKILL / ENGINEER_SKILL สร้างและแก้ไขผ่าน F1 / F3, NOTIFICATION อ่านและแก้ไขผ่าน F4) "
    "ค่าในตารางตรวจสอบอัตโนมัติกับคำสั่ง SQL ทุกคำสั่งแล้ว (88 ช่อง ตรงกันทั้งหมด)",
]
CRUD_F_ROWS = [
    ("F1 Register", "CR", "CR", "C", "", "", "", "", "", "", "", ""),
    ("F2 Login", "R", "", "", "", "", "", "", "", "", "", ""),
    ("F3 Profile", "RU", "CR", "CRD", "", "", "", "", "", "", "", ""),
    ("F4 Notification Bell", "", "", "", "", "R", "", "", "", "", "", "RU"),
]

ER_DESC = [
    "ER Diagram เขียนแบบ Peter Chen มี Entity ตรงกับตาราง CRUD โดย EMPLOYEE เป็น Superclass ของ SALE, PRESALE, PM และ TECH_ENGINEER (Total, Disjoint) "
    "ความสัมพันธ์ 1:M แสดง Foreign Key เป็น attribute เส้นประของ Entity ฝั่ง M และใช้เส้นคู่แทน Total participation",
    "ความสัมพันธ์ M:N มี 2 ความสัมพันธ์ คือ AssignedTo (TECH_ENGINEER – PROJECT) และ Possesses (TECH_ENGINEER – SKILL) "
    "จึงแปลงเป็นตาราง PROJECT_ASSIGNMENT และ ENGINEER_SKILL ใน Logical Schema ส่วน AttachmentFile เป็น Multivalued attribute แปลงเป็นตาราง REQUIREMENT_ATTACHMENT",
]
ERDQ_DESC = ("ERD ที่มี Query แสดงแยกตาม Use Case ในแต่ละรูปตารางแสดงเฉพาะ PK, FK และ attribute ที่ถูก UPDATE (ตัวเอียง, U) "
             "แถวสีเหลืองคือหมายเลข Query ของ Use Case นั้นที่ใช้ตาราง เช่น Q5.2 INSERT หมายถึง Query 5.2 เพิ่มข้อมูลลงตารางนั้น "
             "Query ที่ JOIN หลายตารางจะเดินตามเส้น FK → PK และปรากฏในทุกตารางที่ใช้")

CLASS_DESC = [
    "Class Diagram แบ่งเป็น 3 รูป รูปแรกเป็น Entity Class ตามตารางในฐานข้อมูล แสดง Generalization (EMPLOYEE → SALE / PRESALE / PM / TECH_ENGINEER), "
    "Composition (PROJECT ◆ PROJECT_DOCUMENT, CUSTOMER_REQUIREMENT ◆ REQUIREMENT_ATTACHMENT), Aggregation (PROJECT ◇ TEST_SCHEDULE, PROJECT ◇ CUSTOMER_REQUIREMENT) "
    "และ Association พร้อม multiplicity",
    "อีก 2 รูปเป็น Class ของชั้น Boundary / Controller / API แบ่งตาม UC01–UC04 และ UC05–UC08 ชื่อ Class ตรงกับ Object ใน Sequence Diagram ทุกตัว "
    "Boundary ทุกหน้าสืบทอดจาก AppPage, Controller สืบทอดจาก BaseController (loadProjectList / selectProject ที่ทุก UC ใช้ร่วมกัน), "
    "DetectBlank ของแต่ละ UC สืบทอดจาก DetectBlank และ API สืบทอดจาก BaseAPI ความสัมพันธ์ Boundary → Controller → API เป็น Association "
    "ส่วน Controller ใช้ DetectBlank / File Check และ API ใช้ Entity เป็น Dependency («use»)",
]
STATE_DESC = ("State Diagram ของ Class PROJECT เขียน state เป็น Project.<Status> และ transition เป็น [condition] event / action โดย action คือ Use Case "
              "ที่ทำให้ state เปลี่ยน ใต้ transition แสดงคำสั่ง SQL ที่ update PROJECT.Status ในฐานข้อมูล (ตรงกับ Query ใน Use-Case Narrative With Query)")

PLATFORM_DESC = ("โปรแกรมเป็น Web Application ทำงานบนเครื่องเดียว (local demo) ฝั่ง Client เป็นหน้าเว็บ HTML + JavaScript ล้วน ฝั่ง Server ใช้ Spring Boot 3 "
                 "เรียกฐานข้อมูลผ่าน NamedParameterJdbcTemplate ซึ่งทำหน้าที่เป็น database connect ของทุก <<API>> ในโปรแกรม "
                 "คำสั่ง SQL ทุกคำสั่งอ่านจากไฟล์ ProjectSA_SQL.sql ชุดเดียวกับที่อยู่ใน Use-Case Narrative With Query")
SITEMAP_DESC = ("Site Map มี Login เป็น root ผู้ใช้ไม่ต้องเลือก Role ตอน Login เพราะ Role มาจากบัญชี จากนั้นแตก tree ตามประเภทผู้ใช้ 4 กลุ่ม "
                "แต่ละกลุ่มเห็นเฉพาะเมนู Use Case ของตน และทุกกลุ่มใช้หน้าโปรไฟล์ กระดิ่งแจ้งเตือน และออกจากระบบร่วมกัน")
