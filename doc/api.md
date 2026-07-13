# Prompt สำหรับอธิบายโปรเจค Smart Expiry Date Notification System

คุณคือ Senior Software Architect และ Full-Stack Engineer ที่ต้องออกแบบและสร้างระบบจัดการวัตถุดิบและวันหมดอายุสำหรับร้านอาหาร/ครัวให้ครบทั้ง backend และ frontend โดยแยกหน้าที่กันชัดเจน

โปรเจคนี้มีเป้าหมายหลักคือ
- บันทึกวัตถุดิบในร้านอาหารพร้อมวันหมดอายุ
- ตรวจสอบวัตถุดิบที่ใกล้หมดอายุและหมดอายุแล้ว
- แจ้งเตือนผู้ใช้ล่วงหน้าตามจำนวนวันที่กำหนด
- รองรับการแยกบทบาทผู้ใช้งาน เช่น Manager และ Employee
- รองรับการสแกนวันหมดอายุจากกล้องหรืออัปโหลดรูปภาพ
- รองรับการสร้างเมนูแนะนำจากวัตถุดิบที่ใกล้หมดอายุโดยใช้ AI

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
- Manual and OCR-based ingredient input
- Notification system
- Suggestion system for menu ideas
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
- category
- expiryDate
- notifyDaysBefore
- status: active, used, deleted, expired
- scannedBy
- scannedAt
- updatedBy
- createdAt
- updatedAt

### UsageHistory
- id
- ingredientId
- ingredientName
- actionType: used, deleted, edited, added
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

## Business Rules

- วัตถุดิบทุกชิ้นต้องผูกกับ restaurantId เสมอ
- วัตถุดิบที่มีสถานะ active เท่านั้นที่ต้องนำไปคำนวณแจ้งเตือน
- ถ้า daysLeft < 0 ให้ถือว่า expired
- ถ้า daysLeft <= notifyDaysBefore ให้ถือว่าใกล้หมดอายุ
- เมื่อแก้ไขวัตถุดิบ ต้องอัปเดต updatedAt และ updatedBy
- เมื่อ mark ว่า used หรือ delete ต้องเก็บประวัติการใช้งาน
- การเพิ่มวัตถุดิบจาก OCR ต้องรองรับการแก้ไขข้อมูลก่อนบันทึก
- 1 ร้านอาหารสามารถมีสมาชิกหลายคน แต่ทุกคนต้องอยู่ภายใต้ restaurantId เดียวกัน

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
- เก็บ history ของการใช้งาน
- ตรวจสอบวัตถุดิบใกล้หมดอายุอัตโนมัติ
- สร้าง notification payload สำหรับ frontend หรือ push notification service
- เรียก AI service สำหรับ menu suggestions หากต้องการ

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
- GET /api/ingredients?restaurantId=&status=
- GET /api/ingredients/{id}
- PUT /api/ingredients/{id}
- PATCH /api/ingredients/{id}/used
- PATCH /api/ingredients/{id}/delete
- PATCH /api/ingredients/{id}/status
- GET /api/ingredients/expiring?restaurantId=
- GET /api/ingredients/expired?restaurantId=

Usage History
- POST /api/usage-history
- GET /api/usage-history?restaurantId=
- GET /api/usage-history/{id}

Suggestions
- POST /api/suggestions/menu
- GET /api/suggestions/ingredients/near-expiry?restaurantId=
- GET /api/suggestions/menu/{ingredientName}

Notifications
- GET /api/notifications
- POST /api/notifications/test
- PATCH /api/notifications/{id}/read

OCR / Scan Support
- POST /api/ocr/scan
- POST /api/ocr/scan-image
- POST /api/ocr/extract-expiry-date

### 4) Backend Workflow

#### Register / Login Flow
1. User สมัครสมาชิก
2. Backend ตรวจ role และ restaurant affiliation
3. ถ้าเป็น manager ให้สร้าง restaurant ได้
4. ถ้าเป็น employee ให้เข้าร้านผ่าน invite code
5. Login สำเร็จแล้วส่ง JWT token และข้อมูล role กลับไป

#### Ingredient Add Flow
1. Frontend ส่งข้อมูลวัตถุดิบ
2. Backend validate ชื่อ, category, expiryDate, notifyDaysBefore
3. ระบบบันทึกลงฐานข้อมูลพร้อม createdAt/updatedAt
4. สร้างประวัติการเพิ่มข้อมูล

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

## Frontend Requirements

Frontend ต้องเน้นการใช้งานจริงในร้านอาหาร จึงควรเรียบง่าย เร็ว และกดใช้งานได้ในไม่กี่ขั้นตอน

### 1) Suggested Frontend Modules
- Login screen
- Register screen
- Splash screen
- Dashboard screen
- Ingredient list screen
- Ingredient detail / edit screen
- Scan screen for OCR
- Add ingredient dialog or page
- Suggestions screen
- Profile screen
- Settings screen
- Manager dashboard
- Employee dashboard

### 2) Frontend Features
- แสดงรายการวัตถุดิบแบบ real-time หรือ near real-time
- แสดงสีสถานะตามความเสี่ยงของวันหมดอายุ
- ค้นหาและกรองตาม category
- เรียงลำดับตามวันหมดอายุ
- ปัดลบ / mark as used / edit item
- สแกนวันหมดอายุจากกล้อง
- อัปโหลดรูปจาก gallery
- แสดงคำแนะนำเมนูที่ใช้วัตถุดิบใกล้หมดอายุ
- แสดงการแจ้งเตือนและ history

### 3) Frontend UX Requirements
- UI ต้องใช้งานง่ายสำหรับคนทำงานภาคสนาม
- ปุ่มหลักต้องชัดเจน เช่น Scan, Add, Save, Used, Delete
- ต้องรองรับมือถือเป็นหลัก
- ข้อมูลสำคัญ เช่น days left, status, expiry date ต้องมองเห็นเร็ว
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

## Data Storage Suggestion

ถ้าทำ backend ใหม่ด้วย Spring Boot แนะนำโครงสร้างฐานข้อมูลแบบ relational เช่น

- users
- restaurants
- ingredients
- usage_history
- notifications
- menu_suggestions

ความสัมพันธ์หลัก
- restaurant 1 ต่อ many users
- restaurant 1 ต่อ many ingredients
- ingredient 1 ต่อ many usage_history
- restaurant 1 ต่อ many notifications

## Acceptance Criteria

ระบบถือว่าสมบูรณ์เมื่อ
- ผู้ใช้สมัคร/ล็อกอินได้
- แยก role manager และ employee ได้
- สร้างร้านอาหารและ invite code ได้
- เพิ่ม แก้ไข ลบ เปลี่ยนสถานะวัตถุดิบได้
- ดูรายการวัตถุดิบและสถานะได้
- สแกนวันหมดอายุจาก OCR ได้
- มี job ตรวจสอบวันหมดอายุอัตโนมัติ
- แจ้งเตือนวัตถุดิบใกล้หมดอายุได้
- ดูคำแนะนำเมนูจากวัตถุดิบใกล้หมดอายุได้
- บันทึกประวัติการใช้งานได้

## สิ่งที่ควรเน้นเวลาพัฒนา

- ออกแบบ backend ให้เป็น source of truth
- อย่าให้ frontend คำนวณ business logic สำคัญเพียงอย่างเดียว
- ตรวจสิทธิ์ทุก endpoint ตาม role และ restaurantId
- ทำ validation ทั้งฝั่ง client และ server
- เก็บ audit trail สำหรับการแก้ไขและลบข้อมูล
- ทำ API ให้พร้อมต่อยอดเป็น mobile app และ web app ได้ในอนาคต

## คำสั่งสุดท้ายสำหรับการพัฒนา

ให้พัฒนาโปรเจคนี้เป็นระบบ Smart Expiry Date Notification System ที่มีโครงสร้างแยก frontend/backend ชัดเจน โดย backend ใช้ Java Spring Boot เป็นหลัก รองรับ authentication, restaurant management, ingredient tracking, OCR ingestion, notification scheduling, usage history, และ AI menu suggestion ส่วน frontend ให้เป็น client ที่เรียกใช้งานผ่าน REST API และเน้นประสบการณ์ใช้งานที่รวดเร็ว เข้าใจง่าย และเหมาะกับบริบทของร้านอาหารหรือครัวจริง
