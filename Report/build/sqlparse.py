"""Parse Project Source/ProjectSA_SQL.sql into the schema and the design queries."""
import os, re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SQL_FILE = os.path.join(ROOT, "Project Source", "ProjectSA_SQL.sql")

TABLE_ORDER = ["EMPLOYEE", "SKILL", "ENGINEER_SKILL", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT",
               "TEST_SCHEDULE", "CUSTOMER_REQUIREMENT", "REQUIREMENT_ATTACHMENT", "PROJECT_DOCUMENT",
               "NOTIFICATION"]


def text():
    return open(SQL_FILE, encoding="utf-8-sig").read()


def schema():
    """{table: [ {name, type, null, pk, fk, check, default, comment} ]}"""
    out = {}
    for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\);", text(), re.S):
        t, body = m.group(1), m.group(2)
        cols, pk_multi = [], []
        for line in body.split("\n"):
            line = line.strip()
            if not line or line.startswith("--"):
                continue
            comment = line.split("--", 1)[1].strip() if "--" in line else ""
            line = line.split("--", 1)[0].strip().rstrip(",")
            if line.startswith("PRIMARY KEY"):
                pk_multi = [c.strip() for c in re.search(r"\((.*)\)", line).group(1).split(",")]
                continue
            if line.startswith("CHECK") or line.startswith("("):
                if cols and "CHECK" in line:  # continuation of previous column's CHECK
                    cols[-1]["check"] = (cols[-1]["check"] + " " + line).strip()
                continue
            mm = re.match(r"(\w+)\s+(\w+(?:\([\w,\s]+\))?)(.*)", line)
            if not mm:
                if cols:
                    cols[-1]["check"] = (cols[-1]["check"] + " " + line).strip()
                continue
            name, typ, rest = mm.groups()
            fk = re.search(r"REFERENCES (\w+)\((\w+)\)", rest)
            chk = re.search(r"CHECK \((.*)", rest)
            dflt = re.search(r"DEFAULT (\S+)", rest)
            cols.append(dict(name=name, type=typ, null="NOT NULL" not in rest, pk="PRIMARY KEY" in rest,
                             fk=f"{fk.group(1)}({fk.group(2)})" if fk else "", check=chk.group(1) if chk else "",
                             default=dflt.group(1) if dflt else "", identity="IDENTITY" in rest,
                             unique="UNIQUE" in rest, comment=comment))
        for c in cols:
            if c["name"] in pk_multi:
                c["pk"] = True
            c["check"] = c["check"].strip().rstrip(")").strip() if c["check"].count("(") < c["check"].count(")") else c["check"]
        out[t] = cols
    return out


def queries(only_uc=True):
    """[{uc, ucname, qid, note, sql, tables, ops{table: set}, set_cols{table: [..]}}] in file order."""
    out, uc, ucname = [], None, None
    lines = text().split("\n")
    i = 0
    while i < len(lines):
        ln = lines[i]
        mu = re.match(r"-- (UC\d\d|F\d) (.+?)\s+\(", ln)
        if mu:
            uc, ucname = mu.group(1), mu.group(2).strip()
        mq = re.match(r"-- Query ([\w.]+)(?::\s*(.*))?$", ln)
        if mq and uc:
            qid, note = mq.group(1), (mq.group(2) or "").strip()
            body = []
            i += 1
            while i < len(lines) and not lines[i].startswith("-- Query") and not lines[i].startswith("-- ====="):
                body.append(lines[i])
                i += 1
            sql = "\n".join(body).strip()
            out.append(dict(uc=uc, ucname=ucname, qid=qid, note=note, sql=sql, **analyse(sql)))
            continue
        i += 1
    if only_uc:
        out = [q for q in out if q["uc"].startswith("UC")]
    return out


def analyse(sql):
    code = "\n".join(l.split("--", 1)[0] for l in sql.split("\n"))
    ops, set_cols = {}, {}
    def add(t, op):
        if t in TABLE_ORDER:
            ops.setdefault(t, set()).add(op)
    for t in re.findall(r"INSERT INTO (\w+)", code): add(t, "C")
    for t in re.findall(r"UPDATE (\w+)", code): add(t, "U")
    for t in re.findall(r"DELETE FROM (\w+)", code): add(t, "D")
    for t in re.findall(r"(?:FROM|JOIN) (\w+)", code): add(t, "R")
    for m in re.finditer(r"UPDATE (\w+)\s+SET (.*?)(?:WHERE|;|$)", code, re.S):
        set_cols[m.group(1)] = re.findall(r"(\w+)\s*=", m.group(2))
    return dict(tables=[t for t in TABLE_ORDER if t in ops], ops=ops, set_cols=set_cols)


if __name__ == "__main__":
    import sys, io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    for t, cols in schema().items():
        print(t, [(c["name"], c["type"], "PK" if c["pk"] else "", c["fk"]) for c in cols])
    qs = queries()
    print(len(qs))
    for q in qs:
        print(q["uc"], q["qid"], {k: "".join(sorted(v)) for k, v in q["ops"].items()}, q["set_cols"])
