# Sequence Diagrams: OCR Import & AI Features

เอกสารนี้รวบรวม **Mermaid Plaintext Code** ของ Sequence Diagram สำหรับการทำงานหลัก 3 ส่วน ได้แก่:
1. การนำเข้าวัตถุดิบด้วย OCR (Stock OCR Import)
2. การแนะนำเมนูอาหารจากวัตถุดิบใกล้หมดอายุด้วย AI (AI Menu Suggestion)
3. การวิเคราะห์วางแผนและคาดการณ์วัตถุดิบด้วย AI (AI Ingredient Planning & Forecasting)

คุณสามารถนำโค้ดบล็อกเหล่านี้ไปวางในโปรแกรมที่รองรับ Mermaid (เช่น GitHub, Notion, Obsidian, หรือ [Mermaid Live Editor](https://mermaid.live)) เพื่อแสดงผลเป็นแผนภาพได้ทันที

---

## 1. การนำเข้าวัตถุดิบด้วย OCR (Stock OCR Import Flow)

กระบวนการสแกนรูปภาพฉลากหรือใบเสร็จเพื่อตรวจจับวันหมดอายุและบันทึกเข้าสู่คลังสต็อก

```mermaid
sequenceDiagram
    autonumber
    actor User as User (Employee/Manager)
    participant App as Mobile Frontend (React Native)
    participant API as Backend API (Spring Boot)
    participant OCR as OCR Service (Google Vision / Gemini)
    participant DB as Database (Postgres)

    User->>App: ถ่ายภาพฉลาก/อัปโหลดรูปภาพ
    App->>API: POST /api/ingredients/scan-ocr (ส่งไฟล์รูปภาพ)
    
    Note over API,OCR: ส่งรูปภาพไปประมวลผลข้อความ
    API->>OCR: Request OCR Text Extraction (Image)
    OCR-->>API: Response Raw Extracted Text

    Note over API: ค้นหารายการชื่อวัตถุดิบและวันหมดอายุ<br/>(ด้วย Regex หรือ AI Parsing)
    API-->>App: ส่งกลับข้อมูลร่างวัตถุดิบ (Proposed Name, Expiry Date)
    
    User->>App: ตรวจสอบและแก้ไขข้อมูลบนฟอร์มหน้าจอ
    User->>App: กดปุ่ม "ยืนยันการนำเข้าสต็อก (Confirm)"
    
    App->>API: POST /api/ingredients (ส่งข้อมูลวัตถุดิบที่ต้องการบันทึก)
    API->>DB: บันทึกข้อมูลวัตถุดิบลงในตาราง Ingredient
    API->>DB: บันทึกประวัติการทำรายการลงใน UsageHistory (actionType = ADDED)
    DB-->>API: บันทึกสำเร็จ
    API-->>App: HTTP 201 Created (คืนค่าข้อมูลวัตถุดิบ)
    App-->>User: แสดงข้อความ "นำเข้าวัตถุดิบสำเร็จ" พร้อมอัปเดตหน้าสต็อก
```

---

## 2. การแนะนำเมนูอาหารจากวัตถุดิบใกล้หมดอายุด้วย AI (AI Menu Suggestion Flow)

กระบวนการใช้ AI ดึงข้อมูลวัตถุดิบที่ใกล้หมดอายุในระบบมาคิดไอเดียเมนูอาหาร เพื่อลดขยะอาหาร (Food Waste)

```mermaid
sequenceDiagram
    autonumber
    actor User as User (Employee/Manager)
    participant App as Mobile Frontend (React Native)
    participant API as Backend API (Spring Boot)
    participant DB as Database (Postgres)
    participant AI as AI LLM Service (Gemini / OpenAI API)

    User->>App: เข้าหน้าจอ "แนะนำเมนูอาหารล่วงหน้า (Menu Suggestions)"
    App->>API: GET /api/suggestions/menu?restaurantId={id}
    
    API->>DB: ดึงวัตถุดิบใกล้หมดอายุ (status = ACTIVE และ daysLeft <= notifyDaysBefore)
    DB-->>API: ส่งคืนรายการวัตถุดิบ (เช่น Beef, Spinach, Milk)
    
    Note over API: สร้าง Prompt ส่งให้ AI Model เช่น<br/>"แนะนำเมนู 3 เมนูจากวัตถุดิบเหล่านี้: Beef, Spinach"
    API->>AI: Send Prompt for Recipe Suggestion (JSON format request)
    AI-->>API: Response Suggested Menus (JSON: menuName, steps, matchedIngredients)
    
    Note over API: ตรวจสอบสต็อกเปรียบเทียบหาวัตถุดิบที่ขาด
    API-->>App: ส่งรายการเมนูแนะนำและข้อมูลวัตถุดิบที่ต้องใช้เพิ่ม
    App-->>User: แสดงการ์ดเมนูแนะนำพร้อมวิธีทำและการเช็กสต็อก
```

---

## 3. การวางแผนและคาดการณ์วัตถุดิบด้วย AI (AI Ingredient Planning & Forecasting Flow)

กระบวนการพยากรณ์ปริมาณการใช้วัตถุดิบและแจ้งเตือนวันที่ควรสั่งซื้อของเพิ่ม (Restocking Plan) โดยคำนวณจากประวัติการใช้และอัตราความเร็วในการหมดของวัตถุดิบ

```mermaid
sequenceDiagram
    autonumber
    actor Manager as Manager (User)
    participant App as Mobile Frontend (React Native)
    participant API as Backend API (Spring Boot)
    participant DB as Database (Postgres)
    participant AI as AI Analytics Service (Predictive Model / LLM)

    Manager->>App: เปิดหน้าจอ "วางแผนและคาดการณ์วัตถุดิบ (Inventory Planning)"
    App->>API: GET /api/suggestions/planning?restaurantId={id}
    
    API->>DB: ดึงข้อมูลประวัติการใช้งานย้อนหลัง (UsageHistory - used/deleted)
    API->>DB: ดึงยอดสต็อกคงเหลือปัจจุบัน (Ingredients - active)
    DB-->>API: ส่งข้อมูลประวัติและระดับสต็อก
    
    Note over API: ประมวลผลข้อมูลขั้นต้น<br/>(Consumption Rate per Day/Week)
    API->>AI: ส่งข้อมูลการใช้งานและสต็อกปัจจุบัน (Current Stock + Usage History)
    
    Note over AI: พยากรณ์ระยะเวลาที่สต็อกจะหมด (Out-of-Stock Prediction)<br/>และปริมาณแนะนำสำหรับการสั่งซื้อรอบถัดไป
    AI-->>API: ส่งผลการคาดการณ์ (Forecast Report & Suggested Purchases)
    
    API-->>App: ส่งแผนการสั่งซื้อและแดชบอร์ดคาดการณ์ความเสี่ยงสินค้าขาดมือ
    App-->>Manager: แสดงกราฟวิเคราะห์พยากรณ์ และรายการ shopping list แนะนำ
```
