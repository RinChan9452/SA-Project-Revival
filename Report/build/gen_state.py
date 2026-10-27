"""State Diagram of PROJECT.Status in the Req notation:
state = Class.State, transition = [condition] event / action (action = use case), plus the SQL that writes the
new state (from ProjectSA_SQL.sql). Overwrites Report/State Diagram/State Diagram.drawio.xml."""
import os, re
from html import escape
from drawio import Diagram, TITLE, NOTE
from sqlparse import queries

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(HERE), "State Diagram", "State Diagram.drawio.xml")

STATE = "rounded=1;whiteSpace=wrap;html=1;arcSize=30;fontSize=18;fontStyle=1;"
EDGE = "html=1;endArrow=open;endFill=0;endSize=12;rounded=0;fontSize=12;"
LABEL = "text;html=1;whiteSpace=wrap;align=left;verticalAlign=top;fontSize=16;spacing=2;"


def sql_of(qs, uc, qid, pick=None):
    q = next(q for q in qs if q["uc"] == uc and q["qid"] == qid)
    code = "\n".join(l.split("--", 1)[0] for l in q["sql"].split("\n"))
    stmts = [re.sub(r"\s+", " ", s).strip() + ";" for s in code.split(";") if s.strip()]
    if pick:
        stmts = [s for s in stmts if pick in s]
    return stmts


def label(cond, event, uc, sql_lines):
    sql = "<br>".join(f"{escape(s)}" for s in sql_lines)
    return (f"<b>[{escape(cond)}]</b> {escape(event)} / <b>{escape(uc)}</b><br>"
            f"<font face=\"Courier New\" style=\"font-size:14px\" color=\"#1F4E79\">{sql}</font>")


def main():
    qs = queries()
    up = lambda uc, qid, pick="UPDATE PROJECT": sql_of(qs, uc, qid, pick)
    d = Diagram("State Diagram", "stateDiagramPage1", "sd")
    d.vertex("State Diagram – Project.Status", -330, -60, 800, 40, TITLE)
    S = {}
    def st(key, x, y, w=280):
        S[key] = d.vertex(f"Project.{key}", x, y, w, 64, STATE, cid=key)
    st("NewProject", 200, 200); st("WaitingForAssignEngineer", 900, 200, 320); st("Working", 1600, 200)
    st("Testing", 1600, 700); st("WaitingForApprove", 900, 700, 320); st("Finish", 1600, 1200)
    init = d.vertex("", -60, 217, 30, 30, "ellipse;html=1;fillColor=#000000;strokeColor=#000000;", cid="init")
    end = d.vertex("", 1725, 1400, 30, 30, "ellipse;html=1;shape=endState;fillColor=#000000;strokeColor=#000000;", cid="end")

    d.edge(init, S["NewProject"], EDGE)
    d.vertex(label("Sale กรอกข้อมูลครบและถูกต้อง", "submitProject()", "UC01 Create Project Profile",
                   sql_of(qs, "UC01", "5.2", "INSERT INTO PROJECT")), -360, 40, 540, 150, LABEL)
    d.edge(S["NewProject"], S["WaitingForAssignEngineer"], EDGE)
    d.vertex(label("ข้อมูล Solution ถูกต้อง และมีเอกสาร Solution ≥ 1", "saveSolution()",
                   "UC02 Store Project Solution Details", up("UC02", "9.1")), 490, 90, 400, 110, LABEL)
    d.edge(S["WaitingForAssignEngineer"], S["Working"], EDGE)
    d.vertex(label("เลือก Engineer อย่างน้อย 1 คน", "assignEngineer()", "UC04 Assign Engineer to Project",
                   up("UC04", "9.1")), 1235, 90, 360, 110, LABEL)
    d.edge(S["Working"], S["Testing"], EDGE)
    d.vertex(label("วันทดสอบไม่อยู่ในอดีต และเวลาถูกต้อง", "saveSchedule()",
                   "UC05 Set Project Test Calendar Notification", up("UC05", "7.1")), 1760, 400, 420, 120, LABEL)
    d.edge(S["Testing"], S["Testing"], EDGE + "exitX=1;exitY=0.25;exitDx=0;exitDy=0;entryX=1;entryY=0.75;entryDx=0;entryDy=0;",
           points=[(1950, 716), (1950, 748)])
    d.vertex(label("นัดทดสอบใหม่หลังแก้ไขปัญหา", "saveSchedule()", "UC05 Set Project Test Calendar Notification",
                   sql_of(qs, "UC05", "6.1") + up("UC05", "7.1")), 1970, 660, 470, 150, LABEL)
    d.edge(S["Testing"], S["WaitingForApprove"], EDGE)
    d.vertex(label("ลูกค้าต้องการ Requirement ใหม่", "submitRequirement()", "UC06 Add Customer New Requirement",
                   up("UC06", "9.1")), 1235, 745, 360, 120, LABEL)
    d.edge(S["WaitingForApprove"], S["WaitingForApprove"],
           EDGE + "exitX=0.3;exitY=0;exitDx=0;exitDy=0;entryX=0.7;entryY=0;entryDx=0;entryDy=0;",
           points=[(996, 630), (1124, 630)])
    d.vertex(label("เพิ่ม Requirement อีกข้อในรอบเดียวกัน หรือยังมี Requirement รออนุมัติ (Q11.1 > 0)",
                   "submitRequirement() / submitDecision()", "UC06 Add Customer New Requirement, UC07 Approve New Project Requirement",
                   up("UC06", "9.1")), 760, 470, 600, 130, LABEL)
    d.edge(S["WaitingForApprove"], S["NewProject"], EDGE + "exitX=0;exitY=0.5;exitDx=0;exitDy=0;entryX=0.5;entryY=1;entryDx=0;entryDy=0;",
           points=[(340, 732)])
    d.vertex(label("ไม่มี Requirement รออนุมัติ และอนุมัติ ≥ 1 ข้อในรอบนี้ (Q11.2 > 0)", "submitDecision()",
                   "UC07 Approve New Project Requirement", up("UC07", "11.3")), 360, 750, 520, 130, LABEL)
    d.edge(S["WaitingForApprove"], S["Finish"], EDGE + "exitX=0.5;exitY=1;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;",
           points=[(1060, 1232)])
    d.vertex(label("ไม่มี Requirement รออนุมัติ และไม่อนุมัติทุกข้อในรอบนี้", "submitDecision()",
                   "UC07 Approve New Project Requirement", up("UC07", "11.4")), 1080, 1080, 500, 140, LABEL)
    d.edge(S["Testing"], S["Finish"], EDGE)
    d.vertex(label("Status = Testing, PM ดูแล Project นี้ และไม่มี Requirement รออนุมัติ", "closeProject()",
                   "UC08 Close Project", up("UC08", "6.1")), 1760, 900, 460, 150, LABEL)
    d.edge(S["Finish"], end, EDGE)
    d.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
