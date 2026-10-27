// F4 Notification Bell: 🔔 with an unread badge (N.1, every 30 s), rings (CSS shake) when the count goes up,
// dropdown of the latest 20 (N.2); click = mark read (N.3) and open UC03 for that project; "อ่านทั้งหมด" = N.4.
"use strict";

const Bell = {
  last: null,
  timer: null,

  start() {
    const bell = document.getElementById("bell");
    const panel = document.getElementById("bellPanel");
    if (!bell) return;
    bell.onclick = (e) => {
      e.stopPropagation();
      panel.classList.toggle("open");
      if (panel.classList.contains("open")) Bell.list();
    };
    document.addEventListener("click", (e) => {
      if (!panel.contains(e.target)) panel.classList.remove("open");
    });
    Bell.count();
    Bell.timer = setInterval(Bell.count, 30000);
  },

  async count() {
    try {
      const { Unread } = await App.get("/api/notifications/unread-count");
      const badge = document.getElementById("bellCount");
      badge.textContent = Unread > 99 ? "99+" : Unread;
      badge.classList.toggle("hidden", Unread === 0);
      if (Bell.last !== null && Unread > Bell.last) Bell.ring();
      Bell.last = Unread;
    } catch (e) { /* not logged in any more: app.js redirects */ }
  },

  ring() {
    const bell = document.getElementById("bell");
    bell.classList.remove("ring");
    void bell.offsetWidth;          // restart the animation
    bell.classList.add("ring");
  },

  async list() {
    const panel = document.getElementById("bellPanel");
    const rows = await App.get("/api/notifications");
    panel.innerHTML = `<header><b>การแจ้งเตือน</b><button class="secondary" id="readAll">อ่านทั้งหมด</button></header>` +
      (rows.length ? rows.map((n, i) => `
        <div class="item ${n.IsRead ? "" : "unread"}" data-i="${i}">
          ${App.esc(n.Message)}
          <small>${App.esc(n.ProjectName)} · ${App.dateTime(n.SendAt)}</small>
        </div>`).join("") : `<div class="item"><small>ยังไม่มีการแจ้งเตือน</small></div>`);
    panel.querySelector("#readAll").onclick = async (e) => {
      e.stopPropagation();
      await App.put("/api/notifications/read-all");
      await Bell.count();
      Bell.list();
    };
    panel.querySelectorAll(".item[data-i]").forEach((el) => (el.onclick = async () => {
      const n = rows[el.dataset.i];
      await App.put(`/api/notifications/${n.NotificationID}/read`);
      location.href = `uc03.html?project=${n.ProjectID}`;
    }));
  },
};
