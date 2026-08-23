# สไลด์พรีเซนต์: AI ในระบบ (Menu Suggestion + Purchase Recommendation)

อ้างอิงโค้ดจริง: `suggestion/service/KkuAiClient.java`, `suggestion/service/SuggestionService.java`,
`purchase/service/PurchasePlanningService.java`, `purchase/service/PurchasePlanningScheduler.java`

---

## โครงสไลด์ (แนะนำ 13 สไลด์ ~10-12 นาที)

### Slide 1 — Title
Smart Expiry Date Notification System — ส่วน AI: แนะนำเมนูจากวัตถุดิบใกล้หมดอายุ และแนะนำการซื้อวัตถุดิบ
ชื่อผู้จัดทำ / วันที่

### Slide 2 — ปัญหา (Why)
- ร้านอาหารมีวัตถุดิบใกล้หมดอายุแต่ไม่รู้จะเอาไปทำเมนูอะไร → ทิ้ง = ต้นทุนสูญ
- Manager สั่งซื้อของตาม "ความรู้สึก" → ซื้อเกิน (ของเสีย) หรือซื้อขาด (ขายไม่ได้)
- ข้อมูลสต็อกและประวัติการใช้มีอยู่ในระบบแล้ว แต่ยังไม่ถูกนำมาวิเคราะห์

### Slide 3 — ภาพรวมสถาปัตยกรรม AI
```
Mobile/Web  →  Spring Boot API  →  KkuAiClient  →  https://gen.ai.kku.ac.th/api/v1/chat/completions
                     ↑                                    (OpenAI-compatible, model: gemini-3.5-flash-lite)
              PostgreSQL/Supabase
              (Ingredient, UsageHistory, PurchaseRecommendation)
```
จุดขาย: ใช้ LLM แบบ **OpenAI-compatible** ผ่าน 1 client กลาง (`KkuAiClient`) ใช้ซ้ำได้ทั้ง 2 ฟีเจอร์
API key/base-url/model กำหนดผ่าน env (`KKU_AI_API_KEY`, `KKU_AI_MODEL`) ไม่ hardcode

### Slide 4 — ฟีเจอร์ 1: หน้าแนะนำวัตถุดิบ/เมนู (Menu Suggestion)
3 endpoints (Manager + Employee):
| Endpoint | ใช้ทำอะไร |
|---|---|
| `POST /api/suggestions/menu` | เลือกวัตถุดิบเองหลายตัว → แนะนำเมนู (กำหนด maxMenus, language ได้) |
| `GET /api/suggestions/menu/{ingredientName}` | กดที่วัตถุดิบ 1 ตัว → แนะนำเมนูสูงสุด 5 เมนู |
| `GET /api/suggestions/ingredients/near-expiry` | ลิสต์วัตถุดิบใกล้หมดอายุ + เมนูแนะนำ 3 เมนู/ตัว |

"ใกล้หมดอายุ" = `0 ≤ daysLeft ≤ notifyDaysBefore` ของวัตถุดิบชิ้นนั้น (ตั้งค่าได้รายรายการ)

### Slide 5 — AI ทำงานยังไง (Menu Suggestion)
1. ดึงสต็อกจริงของร้าน (ตัด `DELETED` และ `quantity = 0` ออก)
2. แปลงสต็อกเป็นข้อความ context: ชื่อ, จำนวน, หน่วย, หมวด, วันหมดอายุ, daysLeft
3. System prompt บังคับ **JSON schema** เท่านั้น → `menus[]` มี `menuName, description, ingredientsRequired, ingredientsInStock, missingIngredients, steps, priority, reason`
4. Rules ใน prompt: ห้ามคืน array ว่าง / `ingredientsInStock` ต้องมาจากสต็อกจริง / `priority = HIGH` เมื่อของต้องรีบใช้
5. Client แกะ `choices[0].message.content` → ลอก ```json fence ทิ้ง → parse เป็น DTO

**Prompt Engineering ที่ใช้:** schema-constrained output + grounding ด้วยข้อมูลสต็อกจริง (ลด hallucination) + บังคับภาษา

### Slide 6 — ฟีเจอร์ 2: Purchase Recommendation (Manager only)
| Endpoint | ใช้ทำอะไร |
|---|---|
| `GET/PUT /api/purchase-settings/{restaurantId}` | ตั้งค่ารอบซื้อ |
| `POST /api/purchase-recommendations/generate` | สั่ง generate เดี๋ยวนี้ |
| `GET /api/purchase-recommendations?restaurantId=` | ดูรายการล่าสุด |

Setting: `purchaseDays` (เช่น MON, FRI), `lookbackPurchaseRuns` (ดูย้อนหลังกี่รอบซื้อ, default 4),
`notificationTime` (default 00:01), `safetyBufferPercent` (default 10%)

### Slide 7 — Input ที่ป้อนให้ AI (Purchase)
- **Stock groups**: จับกลุ่มวัตถุดิบด้วย (ชื่อ + หมวด + หน่วย) แล้วรวม quantity
- **Usage history**: เฉพาะ action `CONSUMED` / `USED` ในช่วง lookback
- **ช่วงเวลา lookback** คำนวณย้อนจากวันซื้อจริงตามปฏิทินร้าน (timezone Asia/Bangkok)
- ส่ง purchaseDays / safetyBuffer ให้ AI ใช้ประกอบการตัดสินใจ

Output ต่อรายการ: `recommendedBuyQuantity`, `currentQuantity`, `averageDailyUsage`,
`estimatedConsumptionUntilNextCycle`, `reason` (ภาษาไทย ≤120 ตัวอักษร), `confidence` HIGH/MEDIUM/LOW

### Slide 8 — Automation Flow (Scheduler)
```
cron ทุก 1 นาที (Asia/Bangkok)
  └─ ตรงกับ notificationTime ของร้าน?
       └─ วันนี้เป็นวันซื้อของ (purchaseDays)?
            ├─ สำเร็จ  → generate + แจ้ง Manager: PURCHASE_RECOMMENDATION
            └─ ล้มเหลว → แจ้ง Manager: PURCHASE_RECOMMENDATION_FAILED
```
Manager เปิดแอปตอนเช้าก็เจอ "วันนี้ควรซื้ออะไร เท่าไหร่ เพราะอะไร" ทันที

### Slide 9 — ความน่าเชื่อถือ / Safety ของผลลัพธ์ AI
- Parse แบบ strict: ฟิลด์ข้อความว่าง หรือฟิลด์ตัวเลขไม่ใช่ number → throw ทิ้งทั้งชุด ไม่บันทึกข้อมูลเพี้ยน
- ค่าติดลบถูกบีบเป็น 0, ปัดทศนิยม 3 ตำแหน่ง
- `confidence` ที่ AI ตอบนอกเหนือ HIGH/MEDIUM/LOW → normalize เป็น `LOW`
- ไม่มี API key → error ชัดเจน ไม่เงียบ
- Manager-only + ตรวจ `restaurantId` ตรงกับผู้ใช้ทุก request

### Slide 10 — สถานะ: ✅ ส่วนที่เสร็จแล้ว
- `KkuAiClient` กลาง (chat/completions, JSON cleanup, error mapping) — เสร็จ
- Menu Suggestion ครบ 3 endpoints + logic near-expiry — เสร็จ
- Purchase Settings CRUD + default setting — เสร็จ
- Generate แบบ manual + แบบอัตโนมัติตาม scheduler — เสร็จ
- Notification สำเร็จ/ล้มเหลว ถึง Manager — เสร็จ
- Access control (Manager only / ตรวจร้าน) — เสร็จ
- Integration tests: default setting, employee ถูกปฏิเสธ, generate+save, normalize confidence, AI ตอบพัง, scheduler+notification — เสร็จ
- Swagger/OpenAPI + เอกสาร `doc/completed-03.md` — เสร็จ

### Slide 11 — สถานะ: 🚧 ส่วนที่ต้องทำต่อ
เรียงตามความสำคัญ:
1. **Performance ของหน้า near-expiry** — ตอนนี้เรียก AI 1 ครั้ง *ต่อวัตถุดิบ 1 ตัว* ถ้าของใกล้หมดอายุ 20 ตัว = 20 requests → ช้าและเปลือง ควรยุบเป็น request เดียว หรือทำ cache
2. **Timeout / retry / fallback** — ยังไม่ตั้ง timeout กับ RestClient และไม่มี fallback เมื่อ AI ล่ม (ควรมีสูตรคำนวณแบบ rule-based สำรอง)
3. **เก็บประวัติคำแนะนำ** — ตอน generate จะ `deleteByRestaurantId` ทิ้งของเก่า ทำให้ย้อนดู/เทียบผลไม่ได้
4. **Feedback loop จาก Manager** — ยังไม่มีปุ่ม "ซื้อแล้ว / ปรับจำนวน / ไม่ซื้อ" เพื่อวัดว่า AI แม่นแค่ไหน
5. **การแปลงหน่วย** — จับกลุ่มด้วยหน่วยตรง ๆ (kg กับ g ยังนับแยกกัน)
6. **Frontend หน้าแนะนำวัตถุดิบ + หน้ารายการซื้อ** — ฝั่ง API พร้อมแล้ว UI ยังไม่ได้ทำ
7. **Cost/latency logging** — ยังไม่เก็บ token usage และเวลาตอบของ AI
8. **ประเมินความแม่นยำ** — ยังไม่มีชุดทดสอบเทียบ AI vs สูตรคำนวณจริง
9. **OCR ยังเป็น regex ล้วน** — ยังไม่ได้ใช้ AI ช่วยอ่านฉลาก (ของอนาคต)

### Slide 12 — Roadmap ถัดไป (สั้น ๆ)
- ระยะสั้น (1-2 สัปดาห์): ยุบ N calls เป็น 1, ใส่ timeout + fallback, เก็บประวัติ recommendation
- ระยะกลาง: UI 2 หน้า + ปุ่ม feedback + unit conversion
- ระยะยาว: วัดความแม่นยำ + ลดของเสียเป็นตัวเลข %, AI ช่วย OCR

### Slide 13 — สรุป
- ระบบไม่ได้ "แค่แจ้งเตือนวันหมดอายุ" แต่ต่อยอดเป็น **ใช้ของให้คุ้ม (เมนู)** และ **ซื้อให้พอดี (purchase)**
- AI ถูกใช้แบบมีขอบเขต: grounding ด้วยข้อมูลจริง + บังคับ JSON schema + validate ฝั่ง backend
- Core flow ใช้งานได้จริงแล้ว เหลือ optimize ประสิทธิภาพ ความทนทาน และ UI

---

## Prompt สำหรับให้ AI สร้างสไลด์ (Gamma / Canva AI / Copilot / ChatGPT)

คัดลอกทั้งบล็อกนี้ไปวาง:

```
ช่วยสร้างสไลด์นำเสนอ 13 หน้า ภาษาไทย สำหรับโปรเจกต์นักศึกษาชื่อ
"Smart Expiry Date Notification System" โดยโฟกัสเฉพาะส่วน AI

บริบท: เป็นระบบจัดการวัตถุดิบร้านอาหาร Backend เป็น Java Spring Boot
ต่อกับ LLM ผ่าน API แบบ OpenAI-compatible (endpoint /chat/completions ของ
gen.ai.kku.ac.th, โมเดล gemini-3.5-flash-lite) มีฟีเจอร์ AI 2 ตัว คือ
(1) หน้าแนะนำวัตถุดิบ/เมนู  (2) แนะนำการซื้อวัตถุดิบ (Purchase Recommendation)

สไตล์: โทนสีเขียว-ขาว ดูสะอาด ทันสมัย แบบพรีเซนต์วิชาการ/สตาร์ทอัพ
ใช้ไอคอนประกอบ มีไดอะแกรม flow ในหน้าสถาปัตยกรรมและหน้า scheduler
ทุกหน้าเป็น bullet สั้น ๆ ไม่เกิน 6 บรรทัด ตัวอักษรอ่านง่ายจากหลังห้อง
หน้าไหนที่บอกสถานะงาน ให้ใช้เครื่องหมาย ✅ กับ 🚧 แยกสีชัดเจน

เนื้อหาแต่ละหน้า:
1. หน้าปก: ชื่อโปรเจกต์ + หัวข้อย่อย "AI สำหรับแนะนำเมนูและวางแผนการซื้อวัตถุดิบ"
2. ปัญหา: ของหมดอายุถูกทิ้ง / Manager สั่งของตามความรู้สึก / ข้อมูลมีแต่ไม่ถูกวิเคราะห์
3. สถาปัตยกรรม: Mobile → Spring Boot API → KkuAiClient → LLM, มีฐานข้อมูลเก็บ
   Ingredient, UsageHistory, PurchaseRecommendation (วาดเป็นไดอะแกรมกล่องลูกศร)
4. ฟีเจอร์แนะนำเมนู: 3 API — เลือกวัตถุดิบเอง / กดวัตถุดิบ 1 ตัว / ลิสต์ของใกล้หมดอายุ
   พร้อมเมนูแนะนำ; นิยาม "ใกล้หมดอายุ" คือ daysLeft ไม่เกินค่าที่ตั้งไว้ต่อรายการ
5. AI ทำงานยังไง (เมนู): ดึงสต็อกจริง → ใส่เป็น context → บังคับให้ตอบเป็น JSON schema
   (menuName, ingredientsInStock, missingIngredients, steps, priority, reason) → validate
6. ฟีเจอร์แนะนำการซื้อ: ตั้งค่าวันซื้อของในสัปดาห์, ดูย้อนหลังกี่รอบซื้อ, เวลาแจ้งเตือน,
   safety buffer % (Manager เท่านั้น)
7. ข้อมูลที่ป้อนให้ AI: สต็อกที่จับกลุ่มแล้ว + ประวัติการใช้จริงในช่วง lookback →
   AI ตอบจำนวนที่ควรซื้อ ค่าเฉลี่ยการใช้ต่อวัน เหตุผลภาษาไทย และระดับความมั่นใจ
8. การทำงานอัตโนมัติ: scheduler เช็คทุกนาที ถ้าตรงเวลาและตรงวันซื้อของ → generate →
   ส่งแจ้งเตือนให้ Manager ทั้งกรณีสำเร็จและล้มเหลว (วาดเป็น flowchart)
9. ความน่าเชื่อถือของผลลัพธ์: validate ทุกฟิลด์, ค่าติดลบบีบเป็น 0,
   ความมั่นใจที่ผิดรูปแบบถูกลดเป็น LOW, จำกัดสิทธิ์เฉพาะ Manager
10. ✅ สิ่งที่ทำเสร็จแล้ว: AI client กลาง, แนะนำเมนูครบ 3 API, ตั้งค่ารอบซื้อ,
    generate ทั้งแบบกดเองและอัตโนมัติ, ระบบแจ้งเตือน, ควบคุมสิทธิ์, integration test, เอกสาร API
11. 🚧 สิ่งที่ต้องทำต่อ: ลดจำนวนการเรียก AI ในหน้าของใกล้หมดอายุ, ใส่ timeout/retry/ระบบสำรอง
    เมื่อ AI ล่ม, เก็บประวัติคำแนะนำย้อนหลัง, ปุ่มให้ Manager ยืนยัน/ปรับจำนวน,
    แปลงหน่วย kg-g, ทำหน้าจอ UI, เก็บสถิติค่าใช้จ่ายและเวลาตอบ, วัดความแม่นยำ
12. Roadmap: ระยะสั้น / ระยะกลาง / ระยะยาว เป็น timeline แนวนอน
13. สรุป: จาก "แจ้งเตือนวันหมดอายุ" สู่ "ใช้ของให้คุ้ม + ซื้อให้พอดี",
    AI ถูกใช้แบบมีขอบเขตและตรวจสอบได้, core ใช้งานได้จริงแล้ว

ห้ามใส่ข้อมูลตัวเลขผลลัพธ์ที่ไม่ได้ให้มา และห้ามแต่งฟีเจอร์เพิ่มเอง
```

### เคล็ดลับตอนใช้
- ถ้าใช้ **Gamma**: เลือก "Paste in text" แล้ววางบล็อกด้านบน จะได้ 1 หัวข้อ = 1 สไลด์ตรง ๆ
- ถ้าอยากได้สั้นลงเหลือ 8 หน้า: ตัดสไลด์ 5, 7, 9, 12 ออก (รวมเข้ากับหน้าใกล้เคียง)
- เตรียม screenshot จาก Swagger UI (`/swagger-ui.html`) และตัวอย่าง JSON response
  ใส่ในสไลด์ 5 กับ 7 จะน่าเชื่อถือขึ้นมาก
