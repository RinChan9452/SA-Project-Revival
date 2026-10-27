# -*- coding: utf-8 -*-
"""Checks every query in "Project Source/ProjectSA_SQL.sql" by running the whole project life cycle.

The SQL is T-SQL (SQL Server). This script translates it to SQLite in memory, so it needs nothing but
Python 3.10+ (no database install).  It is a quick design check, not a replacement for running the
queries on H2 / SQL Server (CLAUDE.md §5 step 2).

Flow: F1 register users -> F2 login -> UC01..UC07 (two requirement rounds) -> second project closed by
UC08 -> F3 profile edits -> F4 notification bell.  Every query in the file must run (coverage line).

usage:  python tools/test_sql.py [path-to-sql]      exit code 1 if any check fails
"""
import datetime, os, re, sqlite3, sys
sys.stdout.reconfigure(encoding="utf-8")

DEFAULT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Project Source", "ProjectSA_SQL.sql")


def parse(text):
    """-> (schema text, {section: [(qid, note, sql)]}, [section titles in order])"""
    first = text.index("-- ====")
    schema = text[:first].rstrip() + "\n"
    sections, order, cur = {}, [], None
    for blk in re.split(r"\n(?=-- (?:=+\n|Query ))", "\n" + text[first:]):
        blk = blk.strip("\n")
        m = re.match(r"-- =+\n-- ((?:UC\d\d|F\d)\b[^\n]*)", blk)
        if m:
            cur = m.group(1).split()[0]
            sections[cur] = []
            order.append(m.group(1))
            continue
        m = re.match(r"-- Query ([\w.]+)(?:: ([^\n]*))?\n(.*)", blk, re.S)
        if m and cur:
            body = m.group(3).strip()
            sections[cur].append((m.group(1), m.group(2) or "", None if body == "-- (no statement)" else body))
    return schema, sections, order


path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT
SCHEMA, SECTIONS, _ = parse(open(path, encoding="utf-8-sig").read())

clock = [datetime.datetime(2026, 10, 1, 9, 0)]


def getdate():
    clock[0] += datetime.timedelta(minutes=1)
    return clock[0].strftime("%Y-%m-%d %H:%M:%S")


def tsql_concat(*a):
    return "".join("" if x is None else (str(int(x)) if isinstance(x, float) and x.is_integer() else str(x)) for x in a)


def dateadd_min(n, dt):
    return (datetime.datetime.fromisoformat(dt) + datetime.timedelta(minutes=n)).strftime("%Y-%m-%d %H:%M:%S")


def translate(sql):
    sql = re.sub(r"\bN'", "'", sql)
    sql = re.sub(r"INT IDENTITY\(1,1\) PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT", sql)
    sql = sql.replace("NVARCHAR(MAX)", "NVARCHAR(4000)").replace("DEFAULT GETDATE()", "DEFAULT CURRENT_TIMESTAMP")
    sql = sql.replace("STRING_AGG(", "group_concat(").replace("DATEADD(MINUTE,", "dateadd_min(")
    # SQLite would turn CAST(x AS DATETIME) into a number; the text 'yyyy-mm-dd hh:mm:ss' is what we want
    sql = re.sub(r"CAST\((.+?) AS DATETIME\)", r"(\1)", sql)
    m = re.search(r"\bTOP (\d+) ", sql)
    if m:
        sql = sql.replace(m.group(0), "").rstrip().rstrip(";") + f" LIMIT {m.group(1)};"
    return re.sub(r"@(\w+)", r":\1", sql)


db = sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys = ON")
db.create_function("GETDATE", 0, getdate)
db.create_function("CONCAT", -1, tsql_concat)
db.create_function("dateadd_min", 2, dateadd_min)
db.create_function("LEFT", 2, lambda s, n: None if s is None else str(s)[:n])
db.executescript(translate(SCHEMA))

ran, P = set(), {}


def run(sec, qid, **params):
    P.update(params)
    for q, note, sql in SECTIONS[sec]:
        if q != qid:
            continue
        ran.add((sec, qid))
        if sql is None:
            return None
        rows, cur = None, None
        for stmt in [s.strip() for s in translate(sql).split(";") if s.strip()]:
            m = re.match(r"SET :(\w+) = SCOPE_IDENTITY\(\)", stmt)
            if m:
                P[m.group(1)] = cur.lastrowid
                continue
            cur = db.execute(stmt, {k: v for k, v in P.items() if ":" + k in stmt})
            if stmt.upper().startswith("SELECT"):
                rows = cur.fetchall()
        return rows if rows is not None else cur.rowcount
    raise KeyError(f"{sec} {qid}")


def one(sql, *a):
    return db.execute(sql, a).fetchone()[0]


def status():
    return one("SELECT Status FROM PROJECT WHERE ProjectID = ?", P["ProjectID"])


failed = [0]


def check(label, got, want):
    ok = got == want
    print(("PASS " if ok else "FAIL ") + label + f": {got!r}" + ("" if ok else f"   (want {want!r})"))
    failed[0] += not ok


# ---------------- F1 Register + F2 Login
check("F1 R.1 skills seeded", len(run("F1", "R.1")), 0)
db.executescript("INSERT INTO SKILL (SkillName) VALUES ('CCIE'),('CCNA'),('Java'),('Unix');")
check("F1 R.1 skills list", [r[1] for r in run("F1", "R.1")], ["CCIE", "CCNA", "Java", "Unix"])
users = {}
for first, role, skills in [("Somchai", "Sale", []), ("Pim", "Presale", []), ("Kan", "PM", []),
                            ("Ton", "TechEngineer", [2, 4]), ("Nok", "TechEngineer", [3])]:
    u = first.lower()
    check(f"F1 R.2 {u} free", run("F1", "R.2", Username=u, Email=u + "@x.com")[0][0], 0)
    run("F1", "R.3", FirstName=first, LastName=role, Email=u + "@x.com", Phone=None, Username=u,
        PasswordHash="$2a$hash", Role=role)
    users[u] = P["EmployeeID"]
    for sk in skills:
        run("F1", "R.4", SkillID=sk)
# typed skills: R.5 finds an existing skill ignoring case; not found -> R.6 adds it to SKILL
check("F1 R.5 existing skill, any case", run("F1", "R.5", SkillName="jAVA"), [(3,)])
check("F1 R.5 unknown skill", run("F1", "R.5", SkillName="Docker"), [])
run("F1", "R.6", SkillName="Docker")
check("F1 R.6 new skill found by R.5", run("F1", "R.5", SkillName="docker"), [(P["SkillID"],)])
run("F1", "R.4", EmployeeID=users["nok"], SkillID=P["SkillID"])
check("F1 R.1 suggestions include the typed skill", [r[1] for r in run("F1", "R.1")],
      ["CCIE", "CCNA", "Docker", "Java", "Unix"])
check("F1 R.2 duplicate username refused", run("F1", "R.2", Username="ton", Email="new@x.com")[0][0], 1)
try:
    db.execute("INSERT INTO EMPLOYEE (FirstName, LastName, Email, Username, Password, Role) "
               "VALUES ('A','B','a@x.com','dual','h','Sale/PM')")
    check("one role per account (CHECK)", "accepted", "rejected")
except sqlite3.IntegrityError:
    check("one role per account (CHECK)", "rejected", "rejected")
SALE, PRE, PM, T1, T2 = (users[k] for k in ("somchai", "pim", "kan", "ton", "nok"))
r = run("F2", "L.1", Username="kan")[0]
check("F2 L.1 login returns id + role", (r[0], r[2]), (PM, "PM"))
check("F2 L.1 unknown user", run("F2", "L.1", Username="nobody"), [])

# ---------------- UC01..UC07 (same flow as the earlier test, now through the real file)
run("UC01", "2.1", LoginID=SALE); run("UC01", "2.2"); run("UC01", "2.3")
run("UC01", "5.1", CustomerName="ACME", CustomerEmail="it@acme.com")
run("UC01", "5.2", ProjectName="Core Network", ProjectSite="Bangkok HQ", PresaleID=PRE)
run("UC01", "6.1"); run("UC01", "7.1")
P1 = P["ProjectID"]
check("UC01 -> NewProject", status(), "NewProject")
run("UC02", "1.1", LoginID=PRE); run("UC02", "3.1"); run("UC02", "3.2")
run("UC02", "8.1", StartDate="2026-11-01", EndDate="2026-12-31", Objective="Upgrade", EstimatedBudget=500000,
    SolutionDetail="Cisco core")
run("UC02", "8.2", DocType="Solution", FileName="s.pdf", FilePath="/u/s.pdf", FileSize=10)
run("UC02", "9.1"); run("UC02", "10.1")
check("UC02 -> WaitingForAssignEngineer", status(), "WaitingForAssignEngineer")
for who, role in [(SALE, "Sale"), (PRE, "Presale"), (PM, "PM")]:
    run("UC03", "1.1", LoginID=who, Role=role, Keyword=None)
run("UC03", "3.1", LoginID=SALE, Role="Sale")
for q in ("4.1", "4.2", "4.3", "4.4", "4.5", "4.6"):
    run("UC03", q)
run("UC03", "6.1", DocumentID=1)
run("UC04", "1.1"); run("UC04", "3.1"); run("UC04", "3.2"); run("UC04", "3.3")
check("UC04 5.1 filter Java", [x[1] for x in run("UC04", "5.1", SkillID=3)], ["Nok"])
for e in (T1, T2):
    run("UC04", "8.1", EngineerID=e)
run("UC04", "8.2", LoginID=PM); run("UC04", "9.1"); run("UC04", "10.1")
check("UC04 -> Working", status(), "Working")


def uc05(date):
    run("UC05", "1.1", LoginID=T1); run("UC05", "3.1"); run("UC05", "3.2"); run("UC05", "3.3")
    run("UC05", "6.1")
    run("UC05", "6.2", TestDate=date, StartTime="10:00:00", EndTime="12:00:00", Location="Bangkok HQ",
        TestDetail="UAT", RemindBefore=1440)
    run("UC05", "7.1"); run("UC05", "8.1"); run("UC05", "9.1")


def uc06(title, files=()):
    run("UC06", "1.1", LoginID=PM); run("UC06", "3.1"); run("UC06", "3.2")
    run("UC06", "8.1", Title=title, Detail="d", Priority="High", DueDate="2027-01-15", RelatedFeature=None)
    for f in files:
        run("UC06", "8.2", AttachmentFile=f)
    run("UC06", "9.1"); run("UC06", "10.1")


def uc07(title, dec, reason):
    run("UC07", "1.1", LoginID=SALE)
    rid = [x[0] for x in run("UC07", "3.1") if x[1] == title][0]
    run("UC07", "5.1", RequirementID=rid); run("UC07", "6.1"); run("UC07", "6.2")
    run("UC07", "9.1", Decision=dec, RejectReason=reason); run("UC07", "10.1")
    if run("UC07", "11.1")[0][0] == 0:
        run("UC07", "11.3" if run("UC07", "11.2")[0][0] > 0 else "11.4")


uc05("2026-12-20")
check("UC05 -> Testing", status(), "Testing")
check("UC05 9.1 reminder SendAt = test time - 1440 min",
      one("SELECT SendAt FROM NOTIFICATION WHERE Type = 'TestReminder' ORDER BY NotificationID LIMIT 1"),
      "2026-12-19 10:00:00")
check("UC05 8.1 message shows HH:MM",
      one("SELECT Message FROM NOTIFICATION WHERE Type = 'TestSchedule' ORDER BY NotificationID LIMIT 1"),
      "นัดทดสอบ 2026-12-20 10:00 ที่ Bangkok HQ")
uc06("Add VPN", ("vpn.pdf",)); uc06("Add WiFi")
uc07("Add VPN", "Approved", None)
uc07("Add WiFi", "Rejected", "Out of budget")
check("UC07 round 1 -> NewProject", status(), "NewProject")
check("UC03 6.2 download attachment", run("UC03", "6.2", LoginID=SALE, RequirementID=1, AttachmentFile="vpn.pdf"),
      [("vpn.pdf",)])
run("UC02", "8.1"); run("UC02", "9.1")
run("UC04", "8.1", EngineerID=T1); run("UC04", "9.1")
uc05("2027-01-10"); uc06("Add Monitoring"); uc07("Add Monitoring", "Rejected", "Not in scope")
check("UC07 round 2 -> Finish", status(), "Finish")

# ---------------- UC08 Close Project on a second project
run("UC01", "5.2", ProjectName="Data Center", ProjectSite="Chonburi", PresaleID=PRE, CustomerID=1, LoginID=SALE)
P2 = P["ProjectID"]
run("UC02", "8.1", StartDate="2026-11-01", EndDate="2026-12-31", Objective="DC", EstimatedBudget=1,
    SolutionDetail="x")
run("UC02", "9.1")
for e in (T1, T2):
    run("UC04", "8.1", EngineerID=e)
run("UC04", "8.2", LoginID=PM); run("UC04", "9.1")
check("UC08 1.1 not listed before Testing", [x[0] for x in run("UC08", "1.1", LoginID=PM)], [])
uc05("2027-02-01")
check("UC08 1.1 lists the Testing project", [x[0] for x in run("UC08", "1.1", LoginID=PM)], [P2])
check("UC08 1.1 other PM sees nothing", run("UC08", "1.1", LoginID=SALE), [])
r = run("UC08", "3.1", LoginID=PM)[0]
check("UC08 3.1 sale / presale names", (r[3], r[4]), ("Somchai Sale", "Pim Presale"))
check("UC08 3.2 engineers", len(run("UC08", "3.2")), 2)
check("UC08 3.3 latest test", run("UC08", "3.3")[0][0], "2027-02-01")
check("UC08 5.1 status Testing", run("UC08", "5.1", LoginID=PM)[0][0], "Testing")
check("UC08 5.2 no pending", run("UC08", "5.2")[0][0], 0)
before = one("SELECT COUNT(*) FROM NOTIFICATION")
check("UC08 6.1 updates 1 row", run("UC08", "6.1"), 1)
check("UC08 7.1 schedule Done", run("UC08", "7.1"), 1)
run("UC08", "8.1")
check("UC08 -> Finish", status(), "Finish")
check("UC08 schedule statuses", [s for (s,) in db.execute(
    "SELECT Status FROM TEST_SCHEDULE WHERE ProjectID = ? ORDER BY ScheduleID", (P2,))], ["Done"])
got = sorted(r for (r,) in db.execute("SELECT RecipientID FROM NOTIFICATION WHERE Type = 'ProjectClosed'"))
check("UC08 8.1 notifies Sale, Presale, both engineers", got, sorted([SALE, PRE, T1, T2]))
check("UC08 8.1 message", one("SELECT Message FROM NOTIFICATION WHERE Type='ProjectClosed' LIMIT 1"),
      "Project ปิดแล้ว: Data Center (เตรียมจัดทำเอกสาร UAT และ Handover)")
check("UC08 6.1 refuses when not Testing", run("UC08", "6.1"), 0)
check("UC03 4.3 still shows the test after UC08 (Done)", run("UC03", "4.3")[0][0], "2027-02-01")
check("UC08 5.1 other PM cannot close", run("UC08", "5.1", LoginID=SALE), [])
P["LoginID"] = PM

# ---------------- F3 Profile
r = run("F3", "P.1", LoginID=T1)[0]
check("F3 P.1 own profile + role", (r[0], r[6], r[7]), (T1, "TechEngineer", None))
check("F3 P.2 skills", [x[1] for x in run("F3", "P.2")], ["CCNA", "Unix"])
check("F3 P.3 email taken by other", run("F3", "P.3", Email="kan@x.com")[0][0], 1)
check("F3 P.3 own email ok", run("F3", "P.3", Email="ton@x.com")[0][0], 0)
run("F3", "P.4", FirstName="Tonny", LastName="Tech", Email="tonny@x.com", Phone="0812345678")
run("F3", "P.5", ProfilePicture="/uploads/profile/4.png")
run("F3", "P.6", PasswordHash="$2a$new")
run("F3", "P.7")
for sk in (1, 3):
    run("F1", "R.4", EmployeeID=T1, SkillID=sk)
r = run("F3", "P.1")[0]
check("F3 profile saved; role unchanged", (r[1], r[3], r[6], r[7]),
      ("Tonny", "tonny@x.com", "TechEngineer", "/uploads/profile/4.png"))
check("F3 skills replaced", [x[1] for x in run("F3", "P.2")], ["CCIE", "Java"])
check("F2 login sees new password + picture", run("F2", "L.1", Username="ton")[0][1:6:4],
      ("$2a$new", "/uploads/profile/4.png"))

# ---------------- F4 Notification bell (fake clock is in 2026-10, reminders are in the future)
unread = run("F4", "N.1", LoginID=T1)[0][0]
total_due = one("SELECT COUNT(*) FROM NOTIFICATION WHERE RecipientID = ? AND SendAt <= ?", T1, clock[0].strftime("%Y-%m-%d %H:%M:%S"))
future = one("SELECT COUNT(*) FROM NOTIFICATION WHERE RecipientID = ? AND Type = 'TestReminder'", T1)
check("F4 N.1 counts only notifications already due", unread, total_due)
check("F4 future reminders hidden", future > 0 and unread == one(
    "SELECT COUNT(*) FROM NOTIFICATION WHERE RecipientID=? AND Type<>'TestReminder'", T1), True)
lst = run("F4", "N.2")
check("F4 N.2 newest first, max 20", (len(lst) <= 20, lst[0][3]), (True, "ProjectClosed"))
run("F4", "N.3", NotificationID=lst[0][0])
check("F4 N.3 one read", run("F4", "N.1")[0][0], unread - 1)
check("F4 N.3 cannot mark someone else's", run("F4", "N.3", NotificationID=lst[0][0], LoginID=SALE), 0)
P["LoginID"] = T1
run("F4", "N.4")
check("F4 N.4 all read", run("F4", "N.1")[0][0], 0)

allq = {(s, q) for s, qs in SECTIONS.items() for q, _, sql in qs}
print(f"\nqueries executed: {len(ran)}/{len(allq)}; missing: {sorted(allq - ran) or 'none'}")
print("FAILED:", failed[0])
sys.exit(1 if failed[0] or (allq - ran) else 0)
