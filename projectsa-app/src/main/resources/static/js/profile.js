// F3 Profile (Query P.1–P.7). Role and Username are shown but cannot be changed.
"use strict";

const $ = (id) => document.getElementById(id);
let skillInput = null;

async function load() {
  const d = await App.get("/api/profile");                                    // P.1 (+ P.2, R.1 for Tech)
  const me = d.me;
  $("pic").src = App.avatar(me.ProfilePicture);
  $("name").textContent = `${me.FirstName} ${me.LastName}`;
  $("sub").textContent = `${App.user.RoleName} · ${me.Email}`;
  $("username").value = me.Username;
  $("role").value = App.user.RoleName;
  const f = $("infoForm");
  f.firstName.value = me.FirstName;
  f.lastName.value = me.LastName;
  f.email.value = me.Email;
  f.phone.value = me.Phone || "";
  if (d.allSkills) {                                                        // P.2 = my skills, R.1 = suggestions
    skillInput = App.skillInput($("skills"), d.allSkills, d.skills.map((s) => s.SkillName));
    $("skillCard").classList.remove("hidden");
  }
}

function refreshHeader() {             // the header shows the name and picture from /api/me
  location.reload();
}

(async () => {
  if (!(await App.page())) return;
  await load();

  $("infoForm").onsubmit = async (ev) => {                                    // P.3 + P.4
    ev.preventDefault();
    const form = ev.target, d = App.formData(form), e = {};
    if (!d.firstName.trim()) e.firstName = "กรุณากรอกชื่อ";
    if (!d.lastName.trim()) e.lastName = "กรุณากรอกนามสกุล";
    if (!d.email.trim()) e.email = "กรุณากรอกอีเมล";
    else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.email.trim())) e.email = "รูปแบบอีเมลไม่ถูกต้อง";
    if (Object.keys(e).length) return App.showErrors(form, { fields: e });
    try {
      await App.put("/api/profile", d);
      App.toast("บันทึกข้อมูลแล้ว", "ok");
      setTimeout(refreshHeader, 800);
    } catch (err) { App.showErrors(form, err); }
  };

  $("picForm").onsubmit = async (ev) => {                                     // P.5
    ev.preventDefault();
    const form = ev.target, file = form.picture.files[0];
    if (!file) return App.showErrors(form, { fields: { picture: "กรุณาเลือกไฟล์รูป" } });
    if (!/\.(jpe?g|png)$/i.test(file.name) || file.size > 5 * 1024 * 1024) {
      return App.showErrors(form, { fields: { picture: "รูปโปรไฟล์ต้องเป็น JPG / PNG และขนาดไม่เกิน 5 MB" } });
    }
    try {
      await App.post("/api/profile/picture", new FormData(form));
      App.toast("เปลี่ยนรูปโปรไฟล์แล้ว", "ok");
      setTimeout(refreshHeader, 800);
    } catch (err) { App.showErrors(form, err); }
  };

  $("pwForm").onsubmit = async (ev) => {                                      // L.1 + P.6
    ev.preventDefault();
    const form = ev.target, d = App.formData(form), e = {};
    if (!d.currentPassword) e.currentPassword = "กรุณากรอกรหัสผ่านเดิม";
    if (!d.newPassword) e.newPassword = "กรุณากรอกรหัสผ่านใหม่";
    else if (d.newPassword.length < 6) e.newPassword = "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร";
    if (Object.keys(e).length) return App.showErrors(form, { fields: e });
    try {
      await App.put("/api/profile/password", d);
      form.reset();
      App.toast("เปลี่ยนรหัสผ่านแล้ว", "ok");
    } catch (err) { App.showErrors(form, err); }
  };

  $("skillForm").onsubmit = async (ev) => {                                   // P.7 + R.5 / R.6 + R.4
    ev.preventDefault();
    const form = ev.target, skills = skillInput.values();
    let e = null;
    if (!skills.length) e = "Tech Engineer ต้องกรอก Skill อย่างน้อย 1 รายการ";
    else if (skills.length > 20) e = "กรอก Skill ได้ไม่เกิน 20 รายการ";
    else if (skills.some((s) => s.length > 50)) e = "ชื่อ Skill ต้องยาวไม่เกิน 50 ตัวอักษร";
    if (e) return App.showErrors(form, { fields: { skills: e } });
    try {
      await App.put("/api/profile/skills", { skills });
      App.toast("บันทึก Skill แล้ว", "ok");
    } catch (err) { App.showErrors(form, err); }
  };
})();
