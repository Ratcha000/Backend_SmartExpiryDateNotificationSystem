# Prompt สำหรับอธิบายโปรเจค Smart Expiry Date Notification System

คุณคือ Senior Software Architect และ Full-Stack Engineer ที่ต้องออกแบบและสร้างระบบจัดการวัตถุดิบและวันหมดอายุสำหรับร้านอาหาร/ครัวให้ครบทั้ง backend และ frontend โดยแยกหน้าที่กันชัดเจน

โปรเจคนี้มีเป้าหมายหลักคือ
- บันทึกวัตถุดิบในร้านอาหารพร้อมวันหมดอายุ
- บันทึกปริมาณวัตถุดิบคงเหลือพร้อมหน่วย เช่น กิโลกรัม ขวด ชิ้น แพ็ก
- ตรวจสอบวัตถุดิบที่ใกล้หมดอายุและหมดอายุแล้ว
- แจ้งเตือนผู้ใช้ล่วงหน้าตามจำนวนวันที่กำหนด
- รองรับการแยกบทบาทผู้ใช้งาน เช่น Manager และ Employee
- รองรับการสแกนวันหมดอายุจากกล้องหรืออัปโหลดรูปภาพ
- รองรับการสร้างเมนูแนะนำจากวัตถุดิบที่ใกล้หมดอายุโดยใช้ AI
- รองรับการติดตามการใช้วัตถุดิบบางส่วนก่อนหมดอายุ
- รองรับ AI recommendation สำหรับการวางแผนซื้อวัตถุดิบรอบถัดไป

ระบบเดิมจากแอป Android มีแนวคิดหลักดังนี้
- ใช้ Firebase Authentication สำหรับล็อกอิน/สมัครสมาชิก
- ใช้ Firestore เป็นฐานข้อมูลหลัก
- มีการจัดการผู้ใช้ ร้านอาหาร วัตถุดิบ และประวัติการใช้งาน
- มีการสแกน OCR เพื่ออ่านข้อความวันหมดอายุจากฉลาก/แพ็กเกจ
- มีระบบแจ้งเตือนอัตโนมัติเมื่อวัตถุดิบใกล้หมดอายุหรือหมดอายุแล้ว
- มีหน้าจอแยกสำหรับ Employee และ Manager

## เป้าหมายของงานที่ต้องทำ

ให้สร้างระบบใหม่โดยแยกเป็น 2 ส่วน

1. Backend
- พัฒนาด้วย Java Spring Boot
- ทำหน้าที่เป็น API กลางของระบบ
- ดูแล business logic, authentication, authorization, validation, notification scheduling และ data persistence

2. Frontend
- เป็น client สำหรับผู้ใช้ปลายทาง
- ทำหน้าที่แสดงผลข้อมูล, รับ input จากผู้ใช้, สแกนวันหมดอายุ, ดูรายการวัตถุดิบ, ดูคำแนะนำเมนู, และจัดการข้อมูลผ่าน API
- frontend สามารถเป็น mobile app หรือ web app ได้ แต่ต้องออกแบบให้ใช้งานง่ายสำหรับงานในร้านอาหาร

## ขอบเขตของระบบ

### 1) User Roles
ระบบต้องรองรับอย่างน้อย 2 บทบาท

- Manager
  - สร้างและจัดการร้านอาหาร
  - สร้าง invite code เพื่อเชิญพนักงานเข้าร้าน
  - ดูภาพรวมสต็อกวัตถุดิบ
  - ดูรายการวัตถุดิบใกล้หมดอายุ/หมดอายุ
  - ดูรายงานและประวัติการใช้งาน
  - จัดการสมาชิกในร้าน

- Employee
  - ล็อกอินเข้าระบบ
  - สแกนวันหมดอายุจากกล้องหรืออัปโหลดรูป
  - เพิ่มวัตถุดิบใหม่
  - แก้ไขสถานะวัตถุดิบ เช่น used, deleted, expired
  - ดูรายการวัตถุดิบของร้าน
  - ดูคำแนะนำเมนูจากวัตถุดิบที่ใกล้หมดอายุ

### 2) Core Features
- Login / Register
- Role-based access control
- Restaurant management
- Ingredient management
- Ingredient expiry tracking
- Quantity-based inventory tracking
- Manual and OCR-based ingredient input
- Notification system
- Suggestion system for menu ideas
- Purchase planning and reorder recommendation
- Usage history tracking
- Offline-friendly behavior on client side หากต้องการ

## Domain Model ที่ควรมี

### User
- id / uid
- email
- displayName
- role: manager หรือ employee
- restaurantId

### Restaurant
- id
- name
- managerId
- inviteCode
- createdAt

### Ingredient
- id
- restaurantId
- name
- lotId optional สำหรับผูกวัตถุดิบหลายรายการที่รับเข้ามาพร้อมกันใน lot เดียว
- lotName optional เช่น เนื้อหมู, ไก่, อาหารทะเล
- category
- initialQuantity
- quantity
- unit
- categoryUnitHint optional สำหรับใช้เป็น preset ฝั่ง UI
- expiryDate
- notifyDaysBefore
- status: active, used, deleted, expired
- scannedBy
- scannedAt
- lastUsedAt optional
- updatedBy
- createdAt
- updatedAt

### UsageHistory
- id
- ingredientId
- ingredientName
- actionType: added, edited, consumed, restocked, adjusted, used, deleted
- quantityChanged
- unit
- quantityBefore
- quantityAfter
- performedBy
- performedAt
- restaurantId
- note

### MenuSuggestion
- menuName
- ingredients
- ingredientsInStock
- steps

### IngredientSuggestionGroup
- ingredientName
- daysLeft
- expiryDateFormatted
- menus
- state สำหรับ UI เช่น expanded, loading, hasLoaded

### RestaurantPurchaseSetting
- restaurantId
- buyCycleDays
- nextPlannedPurchaseDate optional
- safetyBufferPercent optional
- updatedAt

### PurchaseRecommendation
- ingredientName
- category
- unit
- currentQuantity
- estimatedConsumptionUntilNextCycle
- recommendedBuyQuantity
- reason
- confidence optional

## Business Rules

- วัตถุดิบทุกชิ้นต้องผูกกับ restaurantId เสมอ
- วัตถุดิบทุกชิ้นต้องมี quantity และ unit ชัดเจน
- quantity ต้องมีค่า >= 0
- initialQuantity ต้องมีค่า >= quantity ตอนสร้างรายการใหม่
- การรับวัตถุดิบหลายชิ้นส่วนในครั้งเดียวควรใช้ batch lot โดยให้วัตถุดิบแต่ละชิ้นเป็น ingredient แยกกัน แต่ใช้ lotId เดียวกัน
- lotId ใช้สำหรับ group รายการที่เข้ามาพร้อมกัน เช่น เนื้อหมู - สันคอหมู, เนื้อหมู - หมูสามชั้น, เนื้อหมู - สะโพกหมู
- วัตถุดิบใน batch lot ใช้ข้อมูลส่วนกลางร่วมกัน เช่น restaurantId, lotName, category, unit, expiryDate, notifyDaysBefore, scannedBy, scannedAt
- quantity ของแต่ละชิ้นส่วนใน batch lot ต้องจัดการแยกกัน เพื่อให้ consume, restock, adjust ได้ตามรายการจริง
- วัตถุดิบที่มีสถานะ active เท่านั้นที่ต้องนำไปคำนวณแจ้งเตือน
- ถ้า daysLeft < 0 ให้ถือว่า expired
- ถ้า daysLeft <= notifyDaysBefore ให้ถือว่าใกล้หมดอายุ
- เมื่อแก้ไขวัตถุดิบ ต้องอัปเดต updatedAt และ updatedBy
- การใช้วัตถุดิบบางส่วนต้องหัก quantity และเก็บประวัติการใช้งาน
- ถ้า quantity เหลือ 0 ให้เปลี่ยนสถานะเป็น used อัตโนมัติ
- ห้าม consume quantity มากกว่าปริมาณคงเหลือ เว้นแต่เป็น flow ปรับสต็อกที่อนุญาตเฉพาะ Manager
- เมื่อ mark ว่า used หรือ delete ต้องเก็บประวัติการใช้งาน
- การเพิ่มวัตถุดิบจาก OCR ต้องรองรับการแก้ไขข้อมูลก่อนบันทึก
- 1 ร้านอาหารสามารถมีสมาชิกหลายคน แต่ทุกคนต้องอยู่ภายใต้ restaurantId เดียวกัน
- buy cycle สำหรับการซื้อวัตถุดิบกำหนดในระดับร้านอาหารสำหรับ v1
- AI reorder recommendation ต้องอิงอย่างน้อย usage history และ buyCycleDays
- category สามารถช่วยแนะนำหน่วยตั้งต้นได้ แต่ unit ที่เก็บใน ingredient record คือ source of truth

## Backend Requirements: Java Spring Boot

ให้ backend เป็นระบบหลักที่รับผิดชอบข้อมูลทั้งหมด ผ่าน REST API และอาจใช้ WebSocket หรือ Scheduled Jobs ตามความเหมาะสม

### 1) Suggested Tech Stack
- Java 17+
- Spring Boot
- Spring Web
- Spring Validation
- Spring Security
- Spring Data JPA หรือ Spring Data MongoDB ตามฐานข้อมูลที่เลือก
- PostgreSQL หรือ MySQL เป็นตัวเลือกหลักสำหรับ relational data
- Redis optional สำหรับ cache / rate limit / session
- Spring Scheduler หรือ Quartz สำหรับงานแจ้งเตือนตามรอบเวลา
- JWT สำหรับ authentication
- Lombok optional
- OpenAPI / Swagger สำหรับเอกสาร API

### 2) Backend Responsibilities
- จัดการสมัครสมาชิกและล็อกอิน
- ออก JWT token
- ตรวจสอบ role ของผู้ใช้
- จัดการร้านอาหารและ invite code
- CRUD วัตถุดิบ
- จัดการ quantity-based inventory และ stock movement
- เก็บ history ของการใช้งาน
- ตรวจสอบวัตถุดิบใกล้หมดอายุอัตโนมัติ
- สร้าง notification payload สำหรับ frontend หรือ push notification service
- เรียก AI service สำหรับ menu suggestions หากต้องการ
- คำนวณแนวโน้มการใช้วัตถุดิบจาก usage history
- สร้างคำแนะนำปริมาณซื้อรอบถัดไปจาก buy cycle และ usage trend

### 3) API ที่ควรมี

Authentication
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- GET /api/auth/me

Users
- GET /api/users/me
- GET /api/users/{id}
- PUT /api/users/{id}
- GET /api/users?restaurantId=

Restaurants
- POST /api/restaurants
- GET /api/restaurants/{id}
- PUT /api/restaurants/{id}
- GET /api/restaurants/invite/{inviteCode}
- POST /api/restaurants/{id}/invite-code
- GET /api/restaurants/{id}/members

Ingredients
- POST /api/ingredients
- POST /api/ingredients/batch
- GET /api/ingredients?restaurantId=&status=&category=
- GET /api/ingredients/{id}
- PUT /api/ingredients/{id}
- PATCH /api/ingredients/{id}/consume
- PATCH /api/ingredients/{id}/restock
- PATCH /api/ingredients/{id}/adjust-quantity
- PATCH /api/ingredients/{id}/used
- PATCH /api/ingredients/{id}/delete
- PATCH /api/ingredients/{id}/status
- GET /api/ingredients/expiring?restaurantId=
- GET /api/ingredients/expired?restaurantId=
- GET /api/ingredients/low-stock?restaurantId=

Usage History
- POST /api/usage-history
- GET /api/usage-history?restaurantId=&ingredientId=&actionType=
- GET /api/usage-history/{id}

Suggestions
- POST /api/suggestions/menu
- GET /api/suggestions/ingredients/near-expiry?restaurantId=
- GET /api/suggestions/menu/{ingredientName}?restaurantId=

Purchase Planning
- GET /api/purchase-settings/{restaurantId}
- PUT /api/purchase-settings/{restaurantId}
- GET /api/purchase-recommendations?restaurantId=
- GET /api/purchase-recommendations/runs?restaurantId=&limit=
- GET /api/purchase-recommendations/runs/{runId}
- POST /api/purchase-recommendations/generate

Notifications
- GET /api/notifications
- POST /api/notifications/test
- PATCH /api/notifications/{id}/read

OCR / Scan Support
- POST /api/ocr/scan
- POST /api/ocr/scan-image
- POST /api/ocr/extract-expiry-date

### 3.1) API Access Matrix

หมายเหตุ
- `Public` คือ endpoint ที่เรียกได้โดยไม่ต้องล็อกอิน
- `Manager, Employee` คือผู้ใช้ที่ล็อกอินแล้วและอยู่ในขอบเขตข้อมูลของร้านที่ตนมีสิทธิ์เข้าถึง
- endpoint ที่เกี่ยวกับการจัดการร้าน, สมาชิก, หรือรายงานเชิงบริหาร ให้ถือว่า Manager เป็นผู้มีสิทธิ์หลัก

| Method | Endpoint | Role ที่เข้าได้ | หน้าที่ |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | สมัครสมาชิกใหม่และกำหนดบทบาทเริ่มต้นของผู้ใช้ |
| POST | `/api/auth/login` | Public | ล็อกอินเพื่อรับ access token และข้อมูลผู้ใช้ |
| POST | `/api/auth/logout` | Manager, Employee | ออกจากระบบโดยให้ client ล้าง token ที่เก็บไว้ |
| GET | `/api/auth/me` | Manager, Employee | ดึงข้อมูลผู้ใช้ที่ล็อกอินอยู่ในปัจจุบัน |
| GET | `/api/users/me` | Manager, Employee | ดูโปรไฟล์ของผู้ใช้ปัจจุบัน |
| GET | `/api/users/{id}` | Manager, Employee | ดูรายละเอียดผู้ใช้ตามรหัสผู้ใช้ |
| PUT | `/api/users/{id}` | Manager, Employee | แก้ไขข้อมูลผู้ใช้ เช่น display name หรือข้อมูลที่ระบบอนุญาต |
| GET | `/api/users?restaurantId=` | Manager | ดูรายชื่อผู้ใช้ โดยสามารถกรองตามร้านอาหารได้ |
| POST | `/api/restaurants` | Manager | สร้างร้านอาหารใหม่และผูก manager เข้ากับร้าน |
| GET | `/api/restaurants/{id}` | Manager, Employee | ดูรายละเอียดร้านอาหารของร้านที่ผู้ใช้สังกัด |
| PUT | `/api/restaurants/{id}` | Manager | แก้ไขข้อมูลร้านอาหาร |
| GET | `/api/restaurants/invite/{inviteCode}` | Public | ตรวจสอบ invite code และดูข้อมูลร้านเบื้องต้นก่อนเข้าร่วม |
| POST | `/api/restaurants/{id}/invite-code` | Manager | สร้างหรือเปลี่ยน invite code สำหรับเชิญพนักงาน |
| GET | `/api/restaurants/{id}/members` | Manager | ดูรายชื่อสมาชิกในร้าน |
| POST | `/api/ingredients` | Manager, Employee | เพิ่มวัตถุดิบใหม่เข้าสู่ระบบของร้าน |
| POST | `/api/ingredients/batch` | Manager, Employee | เพิ่มวัตถุดิบหลายรายการพร้อมกันใน lot เดียว เช่น เพิ่มเนื้อหมูหลายชิ้นส่วนโดยใช้วันหมดอายุและหน่วยร่วมกัน |
| GET | `/api/ingredients?restaurantId=&status=&category=` | Manager, Employee | ดูรายการวัตถุดิบโดยกรองตามร้าน สถานะ และหมวดหมู่ได้ พร้อมข้อมูล quantity และ unit |
| GET | `/api/ingredients/{id}` | Manager, Employee | ดูรายละเอียดวัตถุดิบรายชิ้น รวมทั้ง quantity คงเหลือและ unit |
| PUT | `/api/ingredients/{id}` | Manager, Employee | แก้ไขข้อมูลวัตถุดิบ เช่น ชื่อ หมวดหมู่ quantity unit วันหมดอายุ และการแจ้งเตือน |
| PATCH | `/api/ingredients/{id}/consume` | Manager, Employee | หักปริมาณวัตถุดิบตามจำนวนที่ใช้ และบันทึก quantity before/after |
| PATCH | `/api/ingredients/{id}/restock` | Manager, Employee | เพิ่มปริมาณวัตถุดิบในรายการเดิมสำหรับของที่เติมเข้ามา |
| PATCH | `/api/ingredients/{id}/adjust-quantity` | Manager | ปรับ quantity โดยตรงจากการนับของจริงหรือแก้ข้อมูลผิดพลาด |
| PATCH | `/api/ingredients/{id}/used` | Manager, Employee | ปิดรายการวัตถุดิบเป็น used แบบ explicit เมื่อไม่ใช้ flow consume เป็นหลัก |
| PATCH | `/api/ingredients/{id}/delete` | Manager, Employee | เปลี่ยนสถานะวัตถุดิบเป็น deleted และบันทึกประวัติการใช้งาน |
| PATCH | `/api/ingredients/{id}/status` | Manager, Employee | ปรับสถานะวัตถุดิบตาม workflow ของร้าน |
| GET | `/api/ingredients/expiring?restaurantId=` | Manager, Employee | ดูรายการวัตถุดิบที่ใกล้หมดอายุ |
| GET | `/api/ingredients/expired?restaurantId=` | Manager, Employee | ดูรายการวัตถุดิบที่หมดอายุแล้ว |
| GET | `/api/ingredients/low-stock?restaurantId=` | Manager, Employee | ดูรายการวัตถุดิบที่ปริมาณคงเหลืออยู่ในระดับต่ำเพื่อช่วยวางแผนการซื้อ |
| POST | `/api/usage-history` | Manager, Employee | บันทึกเหตุการณ์การใช้งานหรือการเปลี่ยนแปลงวัตถุดิบ รวมถึง quantity movement |
| GET | `/api/usage-history?restaurantId=&ingredientId=&actionType=` | Manager | ดูประวัติการใช้งานของร้านโดยกรองตามวัตถุดิบหรือประเภทเหตุการณ์ได้ |
| GET | `/api/usage-history/{id}` | Manager | ดูรายละเอียดประวัติการใช้งานแต่ละรายการ |
| POST | `/api/suggestions/menu` | Manager, Employee | ขอเมนูแนะนำจากวัตถุดิบที่มีหรือใกล้หมดอายุ |
| GET | `/api/suggestions/ingredients/near-expiry?restaurantId=` | Manager, Employee | ดูกลุ่มวัตถุดิบใกล้หมดอายุเพื่อใช้เป็นต้นทางของคำแนะนำ |
| GET | `/api/suggestions/menu/{ingredientName}?restaurantId=` | Manager, Employee | ดูเมนูแนะนำที่สัมพันธ์กับวัตถุดิบที่ระบุ โดยใช้ stock ของร้านเพื่อแยกของที่มีและของที่ขาด |
| GET | `/api/purchase-settings/{restaurantId}` | Manager | ดูค่ารอบการซื้อวัตถุดิบของร้าน เช่น buy cycle days |
| PUT | `/api/purchase-settings/{restaurantId}` | Manager | ตั้งค่าหรือแก้ไขรอบการซื้อวัตถุดิบของร้าน |
| GET | `/api/purchase-recommendations?restaurantId=` | Manager | ดูรายการ recommendation ของรอบล่าสุดที่สำเร็จ |
| GET | `/api/purchase-recommendations/runs?restaurantId=&limit=` | Manager | ดูประวัติการ generate ย้อนหลัง สรุปรอบละ 1 รายการ เรียงใหม่ไปเก่า |
| GET | `/api/purchase-recommendations/runs/{runId}` | Manager | ดูรายละเอียดของรอบใดรอบหนึ่ง พร้อมรายการ recommendation ทั้งหมดของรอบนั้น |
| POST | `/api/purchase-recommendations/generate` | Manager | สั่งให้ระบบคำนวณ recommendation ใหม่จาก stock และ usage history ล่าสุด |
| GET | `/api/notifications` | Manager, Employee | ดูรายการการแจ้งเตือนของผู้ใช้หรือของร้าน |
| POST | `/api/notifications/test` | Manager | ทดสอบการสร้างหรือส่ง notification |
| PATCH | `/api/notifications/{id}/read` | Manager, Employee | ทำเครื่องหมายว่าอ่าน notification แล้ว |
| POST | `/api/ocr/scan` | Manager, Employee | สแกนข้อมูลวันหมดอายุจากกล้องหรือแหล่งข้อมูลสด |
| POST | `/api/ocr/scan-image` | Manager, Employee | อัปโหลดรูปภาพเพื่อให้ระบบ OCR อ่านข้อมูล |
| POST | `/api/ocr/extract-expiry-date` | Manager, Employee | แยกและตีความวันหมดอายุจากข้อความหรือผล OCR |

### 3.2) Ingredient Batch Lot Request Example

ใช้สำหรับ flow รับของเข้าร้านที่มีวัตถุดิบหลายชิ้นส่วนในครั้งเดียว เช่น เพิ่มเนื้อหมู 1 lot แล้วเลือกชิ้นส่วนหลายรายการจาก dropdown

```http
POST /api/ingredients/batch
```

```json
{
  "restaurantId": "restaurant-id",
  "lotName": "เนื้อหมู",
  "category": "meat",
  "unit": "kg",
  "categoryUnitHint": "kg",
  "expiryDate": "2026-08-12",
  "notifyDaysBefore": 2,
  "scannedBy": "user-id",
  "scannedAt": "2026-08-06T09:17:25.457Z",
  "items": [
    {
      "partName": "สันคอหมู",
      "initialQuantity": 2,
      "quantity": 2
    },
    {
      "partName": "หมูสามชั้น",
      "initialQuantity": 3,
      "quantity": 3
    },
    {
      "partName": "สะโพกหมู",
      "initialQuantity": 4,
      "quantity": 4
    }
  ]
}
```

ผลลัพธ์คือระบบสร้าง ingredient แยกตามชิ้นส่วน แต่ทุก record ใช้ lotId เดียวกัน เช่น

- เนื้อหมู - สันคอหมู
- เนื้อหมู - หมูสามชั้น
- เนื้อหมู - สะโพกหมู

### 3.3) AI Menu Suggestion Request Example

ระบบเรียก KKU IntelSphere API จาก backend โดยอ่าน config จาก `.env`

```env
KKU_AI_BASE_URL=https://gen.ai.kku.ac.th/api/v1
KKU_AI_API_KEY=your-kku-api-key
KKU_AI_MODEL=gemini-2.5-flash-lite
```

ขอเมนูจากวัตถุดิบหลายรายการ:

```http
POST /api/suggestions/menu
```

```json
{
  "restaurantId": "restaurant-id",
  "ingredientNames": ["Chicken Breast", "Egg"],
  "maxMenus": 5,
  "language": "th"
}
```

ขอเมนูจากวัตถุดิบ 1 รายการ:

```http
GET /api/suggestions/menu/Chicken%20Breast?restaurantId=restaurant-id
```

ดึงวัตถุดิบใกล้หมดอายุพร้อมเมนูแนะนำ:

```http
GET /api/suggestions/ingredients/near-expiry?restaurantId=restaurant-id
```

ผลลัพธ์ของเมนูแนะนำจะมีข้อมูลหลัก:

- menuName
- description
- ingredientsRequired
- ingredientsInStock
- missingIngredients
- steps
- priority
- reason

### 4) Backend Workflow

#### Register / Login Flow
1. User สมัครสมาชิก
2. Backend ตรวจ role และ restaurant affiliation
3. ถ้าเป็น manager ให้สร้าง restaurant ได้
4. ถ้าเป็น employee ให้เข้าร้านผ่าน invite code
5. Login สำเร็จแล้วส่ง JWT token และข้อมูล role กลับไป

#### Ingredient Add Flow
1. Frontend ส่งข้อมูลวัตถุดิบ
2. Backend validate ชื่อ, category, initialQuantity, quantity, unit, expiryDate, notifyDaysBefore
3. ระบบบันทึกลงฐานข้อมูลพร้อม createdAt/updatedAt
4. สร้างประวัติการเพิ่มข้อมูล

#### Ingredient Batch Lot Add Flow
1. ผู้ใช้เลือกประเภทวัตถุดิบหลัก เช่น เนื้อหมู หรือ ไก่
2. Frontend แสดง field ส่วนกลางของ lot เช่น expiryDate, notifyDaysBefore, category, unit
3. ผู้ใช้เลือกชิ้นส่วนจาก dropdown เช่น สันคอหมู, หมูสามชั้น, สะโพกหมู หรือ ปีกบนไก่, น่องไก่
4. ผู้ใช้กดเพิ่มแถวชิ้นส่วนและกรอก quantity ของแต่ละชิ้น
5. Frontend ส่งข้อมูลไปที่ `POST /api/ingredients/batch`
6. Backend สร้าง lotId เดียวกันให้ ingredient ทุกชิ้นใน request
7. Backend สร้าง ingredient แยกตามชิ้นส่วน เช่น `เนื้อหมู - สันคอหมู`, `เนื้อหมู - หมูสามชั้น`
8. Ingredient แต่ละชิ้นยัง consume, restock, adjust, used, delete ได้แยกกัน
9. บันทึก usage history เป็น added แยกตาม ingredient แต่ละรายการ

#### Ingredient Consume Flow
1. Frontend เลือกรายการวัตถุดิบและกรอกจำนวนที่ใช้
2. Backend ตรวจว่า quantity ที่ขอใช้มากกว่า 0 และไม่เกินปริมาณคงเหลือ
3. ระบบหัก quantity คงเหลือ
4. ถ้า quantity เหลือ 0 ให้เปลี่ยนสถานะเป็น used อัตโนมัติ
5. บันทึก usage history พร้อม quantityBefore, quantityChanged, quantityAfter, unit

#### Ingredient Restock / Adjustment Flow
1. Frontend ส่งจำนวนที่ต้องการเพิ่มหรือปรับแก้
2. Backend ตรวจสิทธิ์ของผู้ใช้ โดย adjustment direct quantity ให้ Manager เป็นหลัก
3. ระบบอัปเดต quantity และ updatedAt/updatedBy
4. บันทึก usage history เป็น restocked หรือ adjusted

#### Expiry Check Flow
1. Scheduled job รันทุกวัน
2. Query วัตถุดิบที่ status = active
3. คำนวณ daysLeft จาก expiryDate
4. แยกเป็น expiring และ expired
5. สร้าง notification record หรือส่ง push notification

#### Suggestion Flow
1. ดึงวัตถุดิบที่ใกล้หมดอายุ
2. ส่งข้อมูลวัตถุดิบไปยัง AI service
3. AI คืนเมนูแนะนำ, วัตถุดิบที่ต้องใช้, วัตถุดิบที่มีอยู่, ขั้นตอนทำอาหาร
4. Backend cache ผลลัพธ์ถ้าจำเป็น

#### Purchase Recommendation Flow
1. Manager ตั้งค่า buyCycleDays ของร้าน
2. Backend ดึง usage history และ quantity คงเหลือของวัตถุดิบแต่ละรายการ
3. ระบบคำนวณอัตราการใช้เฉลี่ยต่อวันหรือเฉลี่ยต่อรอบ
4. ประเมินปริมาณที่ต้องใช้จนถึงรอบซื้อครั้งถัดไป
5. AI service หรือ rule engine สร้าง recommendedBuyQuantity พร้อมเหตุผลประกอบ
6. ส่งผลลัพธ์ให้ frontend ใช้เป็นรายการช่วยตัดสินใจก่อนซื้อของเข้าร้าน

## Frontend Requirements

Frontend ต้องเน้นการใช้งานจริงในร้านอาหาร จึงควรเรียบง่าย เร็ว และกดใช้งานได้ในไม่กี่ขั้นตอน

### 1) Suggested Frontend Modules
- Login screen
- Register screen
- Splash screen
- Dashboard screen
- Ingredient list screen
- Ingredient detail / edit screen
- Ingredient consume modal or page
- Ingredient restock / stock adjustment modal
- Ingredient batch lot add screen สำหรับรับของเข้าร้านหลายรายการในครั้งเดียว
- Scan screen for OCR
- Add ingredient dialog or page
- Suggestions screen
- Purchase recommendation screen
- Profile screen
- Settings screen
- Purchase settings screen
- Manager dashboard
- Employee dashboard

### 2) Frontend Features
- แสดงรายการวัตถุดิบแบบ real-time หรือ near real-time
- แสดงสีสถานะตามความเสี่ยงของวันหมดอายุ
- ค้นหาและกรองตาม category
- เรียงลำดับตามวันหมดอายุ
- แสดง quantity คงเหลือและ unit อย่างชัดเจน
- ปัดลบ / mark as used / edit item
- บันทึกการใช้วัตถุดิบบางส่วนด้วยการกรอกจำนวนที่ใช้
- เพิ่ม stock หรือปรับ stock ได้ตามสิทธิ์
- สแกนวันหมดอายุจากกล้อง
- อัปโหลดรูปจาก gallery
- แสดงคำแนะนำเมนูที่ใช้วัตถุดิบใกล้หมดอายุ
- แสดงคำแนะนำปริมาณซื้อรอบถัดไป
- แสดงการแจ้งเตือนและ history

### 3) Frontend UX Requirements
- UI ต้องใช้งานง่ายสำหรับคนทำงานภาคสนาม
- ปุ่มหลักต้องชัดเจน เช่น Scan, Add, Save, Consume, Used, Delete
- ต้องรองรับมือถือเป็นหลัก
- ข้อมูลสำคัญ เช่น days left, status, expiry date, remaining quantity ต้องมองเห็นเร็ว
- ควรมี preset unit ตาม category เช่น เนื้อสัตว์เป็น kg, ซอสเป็น bottle, วัตถุดิบชิ้นนับเป็น pcs
- flow การใช้วัตถุดิบต้องกรอกจำนวนได้เร็วและเห็นยอดคงเหลือทันที
- แสดง empty state และ error state อย่างชัดเจน
- ควรมี loading state ระหว่างดึงข้อมูลหรือสแกน OCR

## OCR / Scan Feature Specification

ระบบสแกนวันหมดอายุควรออกแบบให้รองรับ
- Camera scanning แบบ real-time
- Upload image จาก gallery
- Text recognition ด้วย OCR
- Date pattern detection เช่น dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd, MM/yyyy
- มีการ parse ข้อความที่ขึ้นต้นด้วย keyword เช่น EXP, BBF, วันหมดอายุ
- เมื่อเจอวันที่ ให้แสดง preview ก่อนบันทึกจริง
- ผู้ใช้สามารถแก้ไขวันหมดอายุด้วยตนเองก่อน save

## Notification Specification

ระบบแจ้งเตือนควรมีทั้ง
- แจ้งเตือนรายชิ้น เมื่อวัตถุดิบใกล้หมดอายุ
- แจ้งเตือนเมื่อหมดอายุแล้ว
- แจ้งเตือนสรุปแบบกลุ่มถ้ามีหลายรายการ
- กำหนด notification channel ชัดเจน เช่น expiry_alerts

เงื่อนไขแจ้งเตือน
- expired = daysLeft < 0
- expiring = daysLeft <= notifyDaysBefore
- active items ที่ยังไม่ถึงช่วงแจ้งเตือนไม่ต้องแจ้ง

## AI Menu Recommendation Specification

ระบบแนะนำเมนูควรทำงานดังนี้
- ตรวจวัตถุดิบที่ใกล้หมดอายุ
- วิเคราะห์ว่าวัตถุดิบที่มีอยู่สามารถทำเมนูอะไรได้บ้าง
- แสดงเมนูพร้อมส่วนประกอบ
- แสดงวัตถุดิบที่มีในสต็อกและวัตถุดิบที่ยังขาด
- แสดงขั้นตอนการทำแบบสั้นและอ่านง่าย

ถ้าจะเชื่อม AI service ให้ backend เป็นตัวกลางเรียก AI แทน frontend เพื่อควบคุมข้อมูลและความปลอดภัย

## AI Purchase Recommendation Specification

ระบบแนะนำการซื้อวัตถุดิบควรทำงานดังนี้
- ใช้ quantity คงเหลือปัจจุบันของวัตถุดิบแต่ละรายการ
- ใช้ usage history เพื่อคำนวณแนวโน้มการใช้จริง
- ใช้ buyCycleDays ของร้านเพื่อประเมินว่าต้องมีของพอถึงรอบซื้อถัดไป
- แนะนำ recommendedBuyQuantity พร้อม unit เดิมของวัตถุดิบนั้น
- แสดงเหตุผลประกอบ เช่น ใช้เฉลี่ยสูงขึ้น, stock ปัจจุบันไม่พอถึงรอบถัดไป, หรือมีของใกล้หมดอายุที่ควรระวัง
- เปิดทางให้มี safety buffer ในอนาคต เช่น สั่งเพิ่มอีก 10-20% สำหรับวัตถุดิบที่ใช้ผันผวน

ถ้าระบบยังไม่พร้อมใช้ AI เต็มรูปแบบในช่วงแรก สามารถเริ่มจาก rule-based recommendation ก่อน แล้วค่อยเพิ่ม AI explanation ภายหลังได้

## Data Storage Suggestion

ถ้าทำ backend ใหม่ด้วย Spring Boot แนะนำโครงสร้างฐานข้อมูลแบบ relational เช่น

- users
- restaurants
- ingredients
- usage_history
- restaurant_purchase_settings
- notifications
- menu_suggestions
- purchase_recommendations

ความสัมพันธ์หลัก
- restaurant 1 ต่อ many users
- restaurant 1 ต่อ many ingredients
- lotId 1 ต่อ many ingredients ในกรณีรับวัตถุดิบหลายชิ้นส่วนเข้าพร้อมกัน
- ingredient 1 ต่อ many usage_history
- restaurant 1 ต่อ many notifications
- restaurant 1 ต่อ 1 purchase setting สำหรับ v1

## Acceptance Criteria

ระบบถือว่าสมบูรณ์เมื่อ
- ผู้ใช้สมัคร/ล็อกอินได้
- แยก role manager และ employee ได้
- สร้างร้านอาหารและ invite code ได้
- เพิ่ม แก้ไข ลบ เปลี่ยนสถานะวัตถุดิบได้
- เพิ่มวัตถุดิบพร้อม quantity และ unit ได้
- เพิ่มวัตถุดิบหลายชิ้นส่วนใน lot เดียวได้ เช่น เนื้อหมู 1 lot มีสันคอหมู หมูสามชั้น และสะโพกหมู
- ใช้วัตถุดิบบางส่วนและหัก stock ได้
- ระบบ mark วัตถุดิบเป็น used อัตโนมัติเมื่อ quantity หมดได้
- ดูรายการวัตถุดิบและสถานะได้
- สแกนวันหมดอายุจาก OCR ได้
- มี job ตรวจสอบวันหมดอายุอัตโนมัติ
- แจ้งเตือนวัตถุดิบใกล้หมดอายุได้
- ดูคำแนะนำเมนูจากวัตถุดิบใกล้หมดอายุได้
- บันทึกประวัติการใช้งานได้
- ดูประวัติการใช้วัตถุดิบแบบ quantity movement ได้
- Manager ตั้งค่ารอบการซื้อของร้านได้
- ระบบแนะนำปริมาณซื้อรอบถัดไปได้

## สิ่งที่ควรเน้นเวลาพัฒนา

- ออกแบบ backend ให้เป็น source of truth
- อย่าให้ frontend คำนวณ business logic สำคัญเพียงอย่างเดียว
- ตรวจสิทธิ์ทุก endpoint ตาม role และ restaurantId
- ทำ validation ทั้งฝั่ง client และ server
- เก็บ audit trail สำหรับการแก้ไขและลบข้อมูล
- แยกให้ชัดระหว่าง flow consume, restock, และ manual adjustment
- ทำ quantity และ unit ให้เป็น canonical data ของ ingredient แต่ละรายการ
- ใช้ lotId เป็นข้อมูลสำหรับ group รายการที่รับเข้าพร้อมกัน แต่ไม่แทนที่ ingredientId ของแต่ละวัตถุดิบ
- ทำ API ให้พร้อมต่อยอดเป็น mobile app และ web app ได้ในอนาคต

## คำสั่งสุดท้ายสำหรับการพัฒนา

ให้พัฒนาโปรเจคนี้เป็นระบบ Smart Expiry Date Notification System ที่มีโครงสร้างแยก frontend/backend ชัดเจน โดย backend ใช้ Java Spring Boot เป็นหลัก รองรับ authentication, restaurant management, ingredient tracking, quantity-based inventory, OCR ingestion, notification scheduling, usage history, AI menu suggestion, และ AI purchase recommendation ส่วน frontend ให้เป็น client ที่เรียกใช้งานผ่าน REST API และเน้นประสบการณ์ใช้งานที่รวดเร็ว เข้าใจง่าย และเหมาะกับบริบทของร้านอาหารหรือครัวจริง
