// ProjectSA demo – shared front-end helpers (header, API calls, form errors, project list).
// Each page's own js/ucNN.js holds that use case's <<Controller>> functions from the sequence diagram.
"use strict";

const App = {
  user: null,

  /** fetch wrapper: JSON in/out; throws {status, error, fields} on 4xx/5xx */
  async api(method, url, body) {
    const opt = { method, headers: {} };
    if (body instanceof FormData) {
      opt.body = body;
    } else if (body !== undefined) {
      opt.headers["Content-Type"] = "application/json";
      opt.body = JSON.stringify(body);
    }
    const res = await fetch(url, opt);
    const data = res.headers.get("content-type")?.includes("json") ? await res.json() : null;
    if (!res.ok) {
      if (res.status === 401 && !location.pathname.endsWith("login.html") && !location.pathname.endsWith("register.html")) {
        location.href = "login.html";
      }
      throw { status: res.status, error: data?.error || "เกิดข้อผิดพลาด", fields: data?.fields || {} };
    }
    return data;
  },
  get: (url) => App.api("GET", url),
  post: (url, body) => App.api("POST", url, body),
  put: (url, body) => App.api("PUT", url, body),

  esc(s) {
    return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  },
  date(v) { return v ? String(v).slice(0, 10) : "-"; },
  time(v) { return v ? String(v).slice(0, 5) : "-"; },
  dateTime(v) { return v ? String(v).replace("T", " ").slice(0, 16) : "-"; },
  money(v) { return v == null ? "-" : Number(v).toLocaleString("th-TH", { minimumFractionDigits: 2 }) + " บาท"; },
  status(s) { return `<span class="status ${App.esc(s)}">${App.esc(s)}</span>`; },
  avatar(path) { return path ? App.esc(path) : "img/avatar.svg"; },

  toast(msg, kind = "") {
    const t = document.createElement("div");
    t.className = "toast " + kind;
    t.textContent = msg;
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 3200);
  },

  /** Show DetectBlank results next to each field: {field: message}. Fields are .field[data-f=name] blocks. */
  showErrors(form, err) {
    App.clearErrors(form);
    const fields = err.fields || {};
    let shown = false;
    for (const [name, msg] of Object.entries(fields)) {
      const box = form.querySelector(`.field[data-f="${name}"]`);
      if (box) {
        box.classList.add("err");
        box.querySelector(".msg").textContent = msg;
        shown = true;
      }
    }
    if (!shown || !Object.keys(fields).length) App.toast(err.error || "เกิดข้อผิดพลาด", "bad");
  },
  clearErrors(form) {
    form.querySelectorAll(".field.err").forEach((f) => f.classList.remove("err"));
    form.querySelectorAll(".field .msg").forEach((m) => (m.textContent = ""));
  },

  /** Every logged-in page: load the session user, draw the header (menu, bell, avatar), check the role. */
  async page(roles) {
    try {
      App.user = await App.get("/api/me");
    } catch (e) {
      return null; // api() already redirected to login
    }
    App.header();
    if (roles && !roles.includes(App.user.Role)) {
      document.querySelector("main").innerHTML =
        `<div class="card notice bad">บทบาท ${App.esc(App.user.RoleName)} ไม่มีสิทธิ์ใช้งานหน้านี้</div>`;
      return null;
    }
    return App.user;
  },

  header() {
    const u = App.user;
    const here = location.pathname.split("/").pop();
    const links = u.menu.map((m) =>
      `<a href="${m.page}" class="${m.page === here ? "on" : ""}">${App.esc(m.title)}</a>`).join("");
    const bar = document.createElement("div");
    bar.className = "top";
    bar.innerHTML = `
      <a class="logo" href="index.html">ProjectSA</a>
      <nav>${links}</nav>
      <div class="me">
        <button class="bell" id="bell" title="การแจ้งเตือน">🔔<span class="badge hidden" id="bellCount"></span></button>
        <a href="profile.html" title="โปรไฟล์"><img class="avatar" src="${App.avatar(u.ProfilePicture)}" alt=""></a>
        <div class="who">${App.esc(u.Name)}<br><small>${App.esc(u.RoleName)}</small></div>
        <button class="link" id="logout">ออกจากระบบ</button>
      </div>
      <div class="bell-panel" id="bellPanel"></div>`;
    document.body.prepend(bar);
    bar.querySelector("#logout").onclick = async () => {
      await App.post("/api/logout");
      location.href = "login.html";
    };
    if (window.Bell) Bell.start();
  },

  /**
   * <<Boundary>> ProjectList Form: a table of projects from `url`; clicking a row calls onPick(row).
   * columns: [[header, row => html], ...]
   */
  async projectList(el, url, columns, onPick, emptyText = "ไม่มี Project ในขั้นตอนนี้") {
    const rows = await App.get(url);
    if (!rows.length) {
      el.innerHTML = `<div class="empty">${App.esc(emptyText)}</div>`;
      return rows;
    }
    el.innerHTML = `<table><thead><tr>${columns.map((c) => `<th>${c[0]}</th>`).join("")}</tr></thead><tbody>${
      rows.map((r, i) => `<tr class="pick" data-i="${i}">${columns.map((c) => `<td>${c[1](r)}</td>`).join("")}</tr>`).join("")
    }</tbody></table>`;
    el.querySelectorAll("tr.pick").forEach((tr) => (tr.onclick = () => {
      el.querySelectorAll("tr.sel").forEach((x) => x.classList.remove("sel"));
      tr.classList.add("sel");
      onPick(rows[tr.dataset.i]);
    }));
    return rows;
  },

  /** read a form into an object (checkbox groups with the same name become arrays) */
  /**
   * Typed skills (F1 Register / F3 Profile): type a skill, Enter / comma / "เพิ่ม" turns it into a tag (× removes it).
   * suggestions = SKILL rows (R.1) offered while typing; the server matches names ignoring case (R.5) or adds them (R.6).
   * Returns { values() } – the tags plus any text still in the box.
   */
  skillInput(box, suggestions = [], initial = []) {
    const listId = "skillSuggest" + Math.random().toString(36).slice(2, 8);
    box.innerHTML = `<div class="tags"></div>
      <div class="tag-add"><input list="${listId}" maxlength="50" placeholder="พิมพ์ Skill เช่น Java, Docker แล้วกด Enter" autocomplete="off">
      <button type="button" class="secondary">เพิ่ม</button></div>
      <datalist id="${listId}">${suggestions.map((s) => `<option value="${App.esc(s.SkillName)}">`).join("")}</datalist>`;
    const tags = box.querySelector(".tags"), input = box.querySelector("input");
    let values = [];
    const clean = (s) => s.trim().replace(/\s+/g, " ");
    const draw = () => {
      tags.innerHTML = values.map((v, i) =>
        `<span class="tag">${App.esc(v)}<button type="button" data-i="${i}" title="ลบ">×</button></span>`).join("");
    };
    const add = (text) => {
      for (let name of String(text).split(",").map(clean).filter(Boolean)) {
        const known = suggestions.find((s) => s.SkillName.toLowerCase() === name.toLowerCase());
        if (known) name = known.SkillName;                         // keep the existing spelling
        if (!values.some((v) => v.toLowerCase() === name.toLowerCase())) values.push(name);
      }
      draw();
    };
    input.addEventListener("keydown", (ev) => {
      if (ev.key === "Enter" || ev.key === ",") {
        ev.preventDefault();                                       // Enter must not submit the form
        add(input.value);
        input.value = "";
      }
    });
    box.querySelector(".tag-add button").onclick = () => { add(input.value); input.value = ""; input.focus(); };
    tags.onclick = (ev) => {
      const i = ev.target.dataset.i;
      if (i !== undefined) { values.splice(Number(i), 1); draw(); }
    };
    add(initial.join(","));
    return { values: () => { add(input.value); input.value = ""; return [...values]; } };
  },

  formData(form) {
    const o = {};
    for (const el of form.elements) {
      if (!el.name || el.disabled) continue;
      if (el.type === "checkbox") {
        o[el.name] = o[el.name] || [];
        if (el.checked) o[el.name].push(isNaN(el.value) ? el.value : Number(el.value));
      } else if (el.type === "radio") {
        if (el.checked) o[el.name] = el.value;
      } else if (el.type !== "file") {
        o[el.name] = el.value;
      }
    }
    return o;
  },
};
