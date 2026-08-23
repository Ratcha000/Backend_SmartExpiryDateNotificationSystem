# สไลด์: สรุปงานเฟสปัจจุบัน + แผนเฟสถัดไป

อ้างอิงจากโค้ดจริง `src/main/java/com/app/expiry_system/**` และเอกสาร
`doc/completed-01.md`, `doc/completed-02.md`, `doc/completed-03.md`

ตัวเลขภาพรวมของเฟสนี้: **8 โมดูล / 47 route / 8 controller / 4 integration test**

---

## Slide A — สิ่งที่ทำเสร็จแล้วในเฟสนี้ (Backend Foundation + AI)

**หัวข้อ:** เฟส 1 — ระบบหลังบ้านครบวงจร ตั้งแต่ผู้ใช้ → สต็อก → AI

| # | โมดูล | ทำอะไรได้แล้ว | สถานะ |
|---|---|---|---|
| 1 | Authentication | สมัคร / login / logout / me, JWT + refresh token, role `MANAGER` / `EMPLOYEE` | ✅ |
| 2 | Restaurant | สร้างร้าน, join ด้วย invite code, ดู/แก้ข้อมูลร้าน, รายชื่อสมาชิก, regenerate invite code | ✅ |
| 3 | Ingredient (สต็อก) | เพิ่มทีละชิ้น/แบบ batch, แก้ไข, ลบแบบ soft delete, กรองตาม status/category | ✅ |
| 4 | Stock Movement | `consume` / `restock` / `adjust-quantity` / `used` + อัปเดต status อัตโนมัติ | ✅ |
| 5 | Expiry Tracking | ดึงของ **ใกล้หมดอายุ / หมดอายุแล้ว / ใกล้หมดสต็อก** ตาม `notifyDaysBefore` รายชิ้น | ✅ |
| 6 | Usage History | บันทึกทุกการเคลื่อนไหวของสต็อก + ดูย้อนหลัง (รายงานเป็นสิทธิ์ Manager) | ✅ |
| 7 | AI: แนะนำเมนู | 3 endpoint — เลือกวัตถุดิบเอง / กดที่วัตถุดิบ 1 ตัว / ลิสต์ของใกล้หมดอายุพร้อมเมนู | ✅ |
| 8 | AI: วางแผนซื้อของ | ตั้งค่ารอบซื้อ + generate คำแนะนำปริมาณซื้อจากประวัติการใช้จริง | ✅ |
| 9 | Scheduler | รันอัตโนมัติทุกนาที (`Asia/Bangkok`) ยิงแผนซื้อของตามวัน/เวลาที่ร้านตั้งไว้ | ✅ |
| 10 | Notification | เก็บแจ้งเตือนลง DB + ดึงรายการ + mark as read | ✅ (in-app) |
| 11 | OCR | `POST /api/ocr/extract-expiry-date` และ `/scan` — ดึงวันหมดอายุจากข้อความที่แอปอ่านมา รองรับหลายรูปแบบวันที่ | ✅ |
| 12 | Infra / DevOps | Spring Boot + PostgreSQL (Supabase), Docker, Swagger/OpenAPI, CORS, config ผ่าน env | ✅ |
| 13 | Test + Docs | Integration test 4 โมดูล (ingredient, usage, ocr, purchase) + เอกสาร API ครบทุกกลุ่ม | ✅ |

**จุดขายที่ควรพูดบนเวที**
- AI ใช้ client กลางตัวเดียว (`KkuAiClient`) ใช้ซ้ำได้ทั้ง 2 ฟีเจอร์ ต่อกับ `gen.ai.kku.ac.th` แบบ OpenAI-compatible
- Prompt บังคับตอบเป็น **JSON schema** และ ground ด้วยสต็อกจริงของร้าน → ลด hallucination
- ทุกอย่างที่ AI เสนอ **คนเป็นคนกดยืนยัน** ระบบไม่บันทึกเอง

---

## Slide B — เฟสถัดไปจะทำอะไร

**หัวข้อ:** เฟส 2 — ต่อ Mobile App + ปิดช่องว่างที่เหลือของหลังบ้าน

### 1) Mobile App (React Native) — งานหลักของเฟสถัดไป
มีเอกสารออกแบบไว้แล้ว (`doc/react-native-architecture.md`) แต่ยังไม่ได้ลงมือทำ
- หน้า Login / Register / Join ร้าน
- หน้าสต็อก + หน้าเพิ่มวัตถุดิบ
- **หน้ากล้องสแกนฉลาก** (แอปอ่านตัวอักษร → ส่งข้อความให้ backend หาวันหมดอายุ)
- หน้าแนะนำเมนู และหน้าแผนการซื้อของ

### 2) แจ้งเตือนของจริง
- ตอนนี้ notification เป็นแค่ record ใน database → เฟสหน้าต่อ **Push Notification จริง** (FCM / Expo)
- ตอนนี้มี scheduler แค่ตัวเดียวคือแผนซื้อของ → เพิ่ม **scheduler แจ้งเตือนของใกล้หมดอายุรายวัน**
  (`NotificationType` ตอนนี้มีแค่ `PURCHASE_RECOMMENDATION` กับ `PURCHASE_RECOMMENDATION_FAILED`)

### 3) OCR ฝั่ง server
- `POST /api/ocr/scan-image` ยัง throw ว่า *not enabled yet* → ต่อ Google Vision ให้ backend อ่านภาพเองได้

### 4) ยกระดับระบบสิทธิ์
- ตอนนี้ 1 user ผูก 1 ร้าน และ role เป็น global (`MANAGER` / `EMPLOYEE`)
- เฟสหน้าเปลี่ยนเป็น **membership model**: `restaurant_members` รองรับ 1 Owner + หลาย Manager + หลาย Employee
- เพิ่ม promote / demote / transfer ownership

### 5) ปรับปรุงคุณภาพข้อมูลและ AI
- Pagination + ค้นหาวัตถุดิบตามชื่อ, category master data
- Normalize ชื่อวัตถุดิบ (`Chicken Breast` กับ `อกไก่` ตอนนี้ระบบมองเป็นคนละตัว)
- Cache ผลลัพธ์ AI + ย้ายไปทำเป็น background job (ตอนนี้ near-expiry suggestion เรียก AI หลายรอบ ทำให้ช้า)
- Validate หลัง AI ตอบ ว่าเมนูที่แนะนำใช้ของที่มีในสต็อกจริงหรือไม่

### 6) Non-functional
- เพิ่ม unit test ให้ครอบคลุมมากกว่าเดิม, ตั้ง CI/CD, deploy ขึ้น production
- Export รายงาน usage history + หน้า dashboard สรุปมูลค่าของที่ทิ้งไป

---

## เวอร์ชันสั้นสำหรับใส่สไลด์จริง (ถ้าพื้นที่จำกัด)

**ทำเสร็จแล้ว (เฟส 1) — Backend ครบ 8 โมดูล / 47 endpoint**
✅ ผู้ใช้ + ร้าน + invite code ✅ สต็อก + การเคลื่อนไหว + ประวัติการใช้
✅ ติดตามวันหมดอายุ ✅ AI แนะนำเมนู ✅ AI วางแผนซื้อของ + รันอัตโนมัติตามรอบ
✅ OCR ดึงวันหมดอายุ ✅ ระบบแจ้งเตือนใน DB ✅ Docker + Swagger + Test

**เฟสถัดไป (เฟส 2) — ต่อแอปและปิดช่องว่าง**
🔜 Mobile App React Native ทุกหน้า 🔜 Push Notification จริง + แจ้งเตือนของใกล้หมดอายุรายวัน
🔜 OCR อ่านภาพฝั่ง server 🔜 Owner/Manager/Employee หลายระดับ
🔜 ค้นหา + แบ่งหน้า + จัดกลุ่มชื่อวัตถุดิบ 🔜 Cache/Background job สำหรับ AI
🔜 Dashboard + Export รายงาน 🔜 CI/CD + Deploy

---

## สคริปต์พูด (~60 วินาที)

"เฟสที่ผ่านมาเราโฟกัสที่หลังบ้านให้ครบก่อนครับ ตอนนี้ระบบทำได้ตั้งแต่สมัครสมาชิก สร้างร้าน
ชวนพนักงานเข้าร้าน จัดการสต็อกวัตถุดิบพร้อมบันทึกทุกการเคลื่อนไหว ติดตามวันหมดอายุ
และมี AI สองตัวคือแนะนำเมนูจากของใกล้หมดอายุ กับวางแผนว่ารอบหน้าควรซื้ออะไรเท่าไหร่
ซึ่งตัวหลังรันอัตโนมัติตามวันที่ร้านตั้งไว้ รวมทั้งหมด 8 โมดูล 47 endpoint พร้อม Docker
Swagger และ integration test แล้ว

เฟสถัดไปคือทำแอปมือถือด้วย React Native ต่อทุกหน้าที่ออกแบบไว้ โดยเฉพาะหน้ากล้องสแกนฉลาก
แล้วอัปเกรดการแจ้งเตือนจากที่ตอนนี้เก็บแค่ใน database ให้เป็น push notification จริง
พร้อมเพิ่มการแจ้งเตือนของใกล้หมดอายุรายวัน สุดท้ายคือขยายระบบสิทธิ์ให้รองรับหลาย manager
ต่อร้าน และปรับ AI ให้เร็วขึ้นด้วยการทำ cache ครับ"
