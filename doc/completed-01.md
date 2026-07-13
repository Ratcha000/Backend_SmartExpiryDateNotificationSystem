# Backend API Reference

เอกสารนี้สรุป API ที่มีอยู่จริงใน backend ตอนนี้จาก controller ภายใต้ `expiry-system/src/main/java/com/app/expiry_system`

ขอบเขตของเอกสารนี้:
- รวมทุก route ที่มีอยู่ตอนนี้
- อธิบายหน้าที่ของแต่ละ API
- สรุป request/response ที่ frontend ต้องใช้
- อธิบาย flow การทำงานสำหรับ `MANAGER` และ `EMPLOYEE`

อ้างอิงโค้ดหลัก:
- [AuthController.java]
- [UserController.java]
- [RestaurantController.java]
- [SecurityConfig.java]
- [ApiExceptionHandler.java]

## 1. ภาพรวมระบบ

ระบบตอนนี้มี 3 กลุ่ม API หลัก:
- `auth` สำหรับสมัครสมาชิก, login, logout, ดึงข้อมูลผู้ใช้ที่ login อยู่
- `users` สำหรับดูและแก้ข้อมูลผู้ใช้
- `restaurants` สำหรับสร้างร้าน, ดูร้าน, join ร้าน, ดูสมาชิกในร้าน

role ของผู้ใช้มี 2 แบบ:
- `MANAGER`
- `EMPLOYEE`

ข้อมูลที่ frontend ควรใช้ตัดสิน flow หลัง login:
- `role`
- `restaurantId`

logic หลัก:
- `MANAGER` สมัครและ login ก่อน จากนั้นสร้างร้านด้วย `POST /api/restaurants`
- ตอนสร้างร้านสำเร็จ ระบบจะสร้าง `inviteCode` และส่งกลับมาใน response ทันที
- `EMPLOYEE` สมัครและ login ก่อน จากนั้นใช้ `inviteCode` เพื่อ join ร้านด้วย `POST /api/restaurants/join`

## 2. Authentication และ Common Behavior

### 2.1 Public endpoints

route ต่อไปนี้ไม่ต้องส่ง Bearer token:
- `GET /`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- Swagger docs routes

อ้างอิง: [SecurityConfig.java](/Users/Mac/Project_SmartExpiryDateNotificationSystem/expiry-system/src/main/java/com/app/expiry_system/auth/config/SecurityConfig.java)

### 2.2 Protected endpoints

API อื่นทั้งหมดต้องส่ง header:

```http
Authorization: Bearer <token>
```

### 2.3 Error response format

เมื่อเกิด validation error หรือ business error ระบบจะคืนรูปแบบประมาณนี้:

```json
{
  "timestamp": "2026-07-13T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid invite code"
}
```

หมายเหตุ:
- ตอนนี้ `IllegalArgumentException` หลายกรณีจะถูก map เป็น `400 Bad Request` เหมือนกันทั้งหมด
- ดังนั้น frontend ควรอ่าน `message` ประกอบ ไม่ควรพึ่งแค่ status code อย่างเดียว

## 3. Data Shapes หลัก

### 3.1 UserResponse

```json
{
  "id": "user-id",
  "email": "user@example.com",
  "displayName": "User Name",
  "role": "MANAGER",
  "restaurantId": "restaurant-id"
}
```

### 3.2 RestaurantResponse

```json
{
  "id": "restaurant-id",
  "name": "My Restaurant",
  "managerId": "manager-user-id",
  "inviteCode": "AB12CD34",
  "createdAt": "2026-07-13T14:00:00Z",
  "updatedAt": "2026-07-13T14:00:00Z"
}
```

### 3.3 AuthResponse

```json
{
  "token": "jwt-token",
  "tokenType": "Bearer",
  "user": {
    "id": "user-id",
    "email": "user@example.com",
    "displayName": "User Name",
    "role": "MANAGER",
    "restaurantId": null
  }
}
```

## 4. API ทั้งหมดที่มีอยู่ตอนนี้

## 4.1 Common

### `GET /`

- Auth: ไม่ต้อง login
- หน้าที่: redirect ไปหน้า Swagger UI
- ใช้สำหรับ: developer / backend testing

response:
- HTTP `302 Found`
- redirect ไป `/swagger-ui.html`

---

## 4.2 Auth APIs

### `POST /api/auth/register`

- Auth: ไม่ต้อง login
- หน้าที่: สมัครสมาชิกใหม่
- เหมาะกับ: หน้า register

request body:

```json
{
  "email": "manager@example.com",
  "password": "password123",
  "displayName": "Manager Name",
  "role": "MANAGER",
  "restaurantId": null
}
```

validation/behavior:
- `email` ต้องเป็นรูปแบบ email
- `password` ยาว 8-100 ตัวอักษร
- `displayName` ห้ามว่าง
- `role` ห้ามว่าง
- ตอนนี้ห้าม assign `restaurantId` ตอนสมัคร ถ้าส่งค่ามาจะ error

success response:
- HTTP `201 Created`
- คืน `AuthResponse`

frontend note:
- register สำเร็จแล้วจะได้ token กลับมาเลย
- สามารถถือว่า user login แล้วทันที

### `POST /api/auth/login`

- Auth: ไม่ต้อง login
- หน้าที่: login ด้วย email/password
- เหมาะกับ: หน้า login

request body:

```json
{
  "email": "manager@example.com",
  "password": "password123"
}
```

success response:
- HTTP `200 OK`
- คืน `AuthResponse`

frontend note:
- เก็บ `token`
- ใช้ `user.role` และ `user.restaurantId` ตัดสินหน้าแรกหลัง login

### `POST /api/auth/logout`

- Auth: ไม่ต้อง login ตาม config ปัจจุบัน
- หน้าที่: จบ session ฝั่ง client แบบ stateless
- เหมาะกับ: ปุ่ม logout

success response:
- HTTP `204 No Content`

frontend note:
- backend ไม่มี token blacklist
- การ logout จริง ๆ คือ frontend ลบ token ฝั่ง client

### `GET /api/auth/me`

- Auth: ต้อง login
- หน้าที่: ดึงข้อมูล user ของคนที่ login อยู่
- เหมาะกับ: auth bootstrap, profile, app init

success response:
- HTTP `200 OK`
- คืน `UserResponse`

frontend note:
- ใช้ route นี้เช็กว่า token ใช้งานได้หรือไม่
- ใช้เป็น source of truth ของ user ปัจจุบันได้

---

## 4.3 User APIs

### `GET /api/users/me`

- Auth: ต้อง login
- หน้าที่: ดึงข้อมูล user ปัจจุบัน
- หมายเหตุ: ทำงานซ้ำกับ `GET /api/auth/me`

success response:
- HTTP `200 OK`
- คืน `UserResponse`

frontend recommendation:
- เลือกใช้เพียง route เดียวระหว่าง `/api/auth/me` กับ `/api/users/me`

### `GET /api/users/{id}`

- Auth: ต้อง login
- หน้าที่: ดูรายละเอียด user ตาม `id`
- เหมาะกับ: หน้า member detail, admin/member management

path params:
- `id`: user id

success response:
- HTTP `200 OK`
- คืน `UserResponse`

important note:
- ตอนนี้ service ยังไม่จำกัดสิทธิ์การดู user รายคนอย่างเข้มงวด
- frontend ไม่ควรเปิด UI นี้ให้เข้าถึงแบบกว้างโดยไม่จำเป็น

### `PUT /api/users/{id}`

- Auth: ต้อง login
- หน้าที่: แก้ข้อมูล user
- เหมาะกับ: edit profile

path params:
- `id`: user id ที่ต้องการแก้

request body:

```json
{
  "displayName": "New Name",
  "role": "EMPLOYEE",
  "restaurantId": null
}
```

behavior:
- user แก้ตัวเองได้
- manager แก้ user ที่อยู่ร้านเดียวกันได้
- ถ้าส่ง `role` จะถูกอนุญาตเฉพาะกรณี user แก้ตัวเอง
- ถ้าส่ง `restaurantId` ตอนนี้ระบบจะ reject และให้ไปใช้ restaurant APIs แทน

success response:
- HTTP `200 OK`
- คืน `UserResponse`

frontend note:
- ใช้ API นี้แก้ `displayName` ได้
- ไม่ควรใช้ API นี้สำหรับ join/leave restaurant

### `GET /api/users`

- Auth: ต้อง login
- หน้าที่: ดึงรายชื่อผู้ใช้
- เหมาะกับ: หน้ารายชื่อสมาชิก, หน้าจัดการผู้ใช้

query params:
- `restaurantId` เป็น optional

ตัวอย่าง:
- `GET /api/users`
- `GET /api/users?restaurantId=<restaurantId>`

success response:
- HTTP `200 OK`
- คืน `UserResponse[]`

important note:
- ถ้าไม่ส่ง `restaurantId` ตอนนี้จะคืน user ทั้งระบบ
- ถ้าส่ง `restaurantId` จะ filter ตามร้าน
- สำหรับ frontend ฝั่งร้าน ควร prefer ใช้ `GET /api/restaurants/{id}/members` มากกว่า เพราะ intent ชัดกว่า

---

## 4.4 Restaurant APIs

### `POST /api/restaurants`

- Auth: ต้อง login
- หน้าที่: สร้างร้านใหม่
- ใช้โดย: `MANAGER` เท่านั้น
- เหมาะกับ: หน้า create restaurant หลัง manager login ครั้งแรก

request body:

```json
{
  "name": "My Restaurant"
}
```

validation/behavior:
- `name` ห้ามว่าง
- `name` ยาวไม่เกิน 100 ตัวอักษร
- เฉพาะ `MANAGER` สร้างได้
- manager ที่มี `restaurantId` อยู่แล้วจะสร้างซ้ำไม่ได้
- manager ที่เป็น owner ร้านอยู่แล้วจะสร้างซ้ำไม่ได้
- ระบบสร้าง `inviteCode` ให้ทันที
- ระบบจะ update `restaurantId` ของ manager ให้เป็นร้านที่เพิ่งสร้าง

success response:
- HTTP `201 Created`
- คืน `RestaurantResponse`

frontend note:
- หลังสร้างสำเร็จ ให้ดึง `inviteCode` จาก response ไปแสดงได้ทันที
- manager ไม่ต้องกดสร้าง invite code แยกอีกรอบ

### `GET /api/restaurants/{id}`

- Auth: ต้อง login
- หน้าที่: ดึงข้อมูลร้านตาม `id`
- เหมาะกับ: restaurant profile, restaurant settings, dashboard setup

path params:
- `id`: restaurant id

behavior:
- user ต้องอยู่ในร้านนี้จึงจะดูได้

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse`

### `GET /api/restaurants/me`

- Auth: ต้อง login
- หน้าที่: ดึงข้อมูลร้านของ user ปัจจุบัน
- เหมาะกับ: app bootstrap หลัง login, dashboard entry, settings

behavior:
- ถ้า user ยังไม่มี `restaurantId` จะ error

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse`

frontend recommendation:
- route นี้เหมาะที่สุดสำหรับโหลดร้านปัจจุบันโดยไม่ต้องเดา `restaurantId`

### `PUT /api/restaurants/{id}`

- Auth: ต้อง login
- หน้าที่: แก้ข้อมูลร้าน
- ใช้โดย: manager ของร้านนั้นเท่านั้น
- เหมาะกับ: restaurant settings

request body:

```json
{
  "name": "Updated Restaurant Name"
}
```

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse`

### `GET /api/restaurants/invite/{inviteCode}`

- Auth: ต้อง login
- หน้าที่: lookup ร้านจาก invite code
- เหมาะกับ: หน้า join restaurant ก่อนกดยืนยัน

path params:
- `inviteCode`: รหัสเชิญ

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse`

frontend note:
- ใช้เพื่อ preview ว่า invite code นี้เป็นร้านอะไร
- ตอนนี้ response ส่ง `inviteCode` กลับมาด้วย

### `POST /api/restaurants/join`

- Auth: ต้อง login
- หน้าที่: ให้ employee เข้าร่วมร้านด้วย invite code
- ใช้โดย: `EMPLOYEE` เท่านั้น
- เหมาะกับ: หน้า join restaurant

request body:

```json
{
  "inviteCode": "AB12CD34"
}
```

behavior:
- เฉพาะ `EMPLOYEE`
- user ที่มี `restaurantId` อยู่แล้วจะ join ซ้ำไม่ได้
- ถ้า invite code ไม่ถูกต้องจะ error
- สำเร็จแล้วระบบจะ update `currentUser.restaurantId`

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse` ของร้านที่เข้าร่วม

frontend note:
- หลัง join สำเร็จ ควร refresh ข้อมูล user ด้วย `/api/auth/me` หรือใช้ข้อมูลร้านจาก response แล้วเปลี่ยน route เข้า dashboard

### `POST /api/restaurants/{id}/invite-code`

- Auth: ต้อง login
- หน้าที่: regenerate invite code ของร้าน
- ใช้โดย: manager ของร้านนั้นเท่านั้น
- เหมาะกับ: restaurant settings หรือ invite management

path params:
- `id`: restaurant id

behavior:
- สร้าง invite code ใหม่
- code ใหม่จะถูกบันทึกแทน code เดิม

success response:
- HTTP `200 OK`
- คืน `RestaurantResponse` พร้อม `inviteCode` ใหม่

frontend note:
- ถ้า manager กด regenerate แล้ว frontend ควรอัปเดต code ที่แสดงทันที

### `GET /api/restaurants/{id}/members`

- Auth: ต้อง login
- หน้าที่: ดึงรายชื่อสมาชิกของร้าน
- เหมาะกับ: member list, team management

path params:
- `id`: restaurant id

behavior:
- user ต้องอยู่ในร้านนี้จึงจะเรียกได้

success response:
- HTTP `200 OK`
- คืน `UserResponse[]`

frontend recommendation:
- ใช้ route นี้แทน `GET /api/users?restaurantId=...` เมื่อจุดประสงค์คือ “สมาชิกของร้านนี้”

## 5. Frontend Flow ที่แนะนำ

## 5.1 Register Flow

### Manager

1. เรียก `POST /api/auth/register`
2. ได้ `token` และ `user`
3. ถ้า `role = MANAGER` และ `restaurantId = null` ให้พาไปหน้า `Create Restaurant`
4. เรียก `POST /api/restaurants`
5. ได้ `RestaurantResponse` พร้อม `inviteCode`
6. แสดงหน้า setup success หรือ dashboard พร้อม invite code

### Employee

1. เรียก `POST /api/auth/register`
2. ได้ `token` และ `user`
3. ถ้า `role = EMPLOYEE` และ `restaurantId = null` ให้พาไปหน้า `Join Restaurant`
4. ถ้าต้อง preview ร้าน ให้เรียก `GET /api/restaurants/invite/{inviteCode}`
5. กดยืนยันแล้วเรียก `POST /api/restaurants/join`
6. สำเร็จแล้วพาเข้า dashboard

## 5.2 Login Flow

1. เรียก `POST /api/auth/login`
2. เก็บ `token`
3. อ่าน `user.role` และ `user.restaurantId`
4. route ตามนี้

กรณีที่แนะนำ:
- `MANAGER` + `restaurantId = null` -> ไป `Create Restaurant`
- `MANAGER` + `restaurantId != null` -> เข้า manager dashboard
- `EMPLOYEE` + `restaurantId = null` -> ไป `Join Restaurant`
- `EMPLOYEE` + `restaurantId != null` -> เข้า employee dashboard

## 5.3 App Bootstrap Flow

เมื่อเปิดแอปและมี token อยู่แล้ว:

1. เรียก `GET /api/auth/me`
2. ถ้า token ใช้ไม่ได้ ให้กลับหน้า login
3. ถ้าใช้ได้ ให้เช็ก `role` และ `restaurantId`
4. ถ้าต้องใช้ข้อมูลร้านทันที ให้เรียก `GET /api/restaurants/me`

## 5.4 Restaurant Management Flow

### Manager ดูข้อมูลร้าน

1. เรียก `GET /api/restaurants/me`
2. แสดงชื่อร้าน, invite code, วันที่สร้าง, วันที่อัปเดต
3. ถ้าต้องการรายชื่อสมาชิก ให้เรียก `GET /api/restaurants/{id}/members`

### Manager เปลี่ยนชื่อร้าน

1. เรียก `PUT /api/restaurants/{id}`
2. อัปเดต state จาก response

### Manager เปลี่ยน invite code

1. เรียก `POST /api/restaurants/{id}/invite-code`
2. แสดง code ใหม่จาก response

## 5.5 Profile Flow

1. โหลดข้อมูลจาก `GET /api/auth/me` หรือ `GET /api/users/me`
2. แก้ชื่อด้วย `PUT /api/users/{id}`
3. ไม่ใช้ API users สำหรับเปลี่ยนร้าน

## 6. Route ที่ frontend ควรใช้เป็นหลัก

ถ้าจะทำ frontend ให้เรียบง่าย แนะนำใช้ route เหล่านี้เป็นหลัก:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/restaurants`
- `GET /api/restaurants/me`
- `GET /api/restaurants/invite/{inviteCode}`
- `POST /api/restaurants/join`
- `GET /api/restaurants/{id}/members`
- `PUT /api/restaurants/{id}`
- `POST /api/restaurants/{id}/invite-code`
- `PUT /api/users/{id}`

route ที่มีอยู่แต่ frontend อาจไม่จำเป็นต้องใช้ก่อน:
- `GET /api/users/{id}`
- `GET /api/users`
- `GET /api/users/me`
- `GET /api/restaurants/{id}`
- `POST /api/auth/logout`

## 7. ข้อสังเกตสำคัญสำหรับ frontend

- ตอนนี้ `logout` เป็นแค่ `204 No Content` ไม่มี server-side session invalidation
- ตอนนี้หลาย business error คืน `400` เหมือนกันหมด ควรอ่าน `message`
- `GET /api/auth/me` และ `GET /api/users/me` ซ้ำหน้าที่กัน เลือกใช้เส้นเดียว
- manager จะได้ `inviteCode` กลับมาทันทีตั้งแต่ตอนสร้างร้าน
- employee ไม่สามารถ join ร้านตอน register ได้ ต้อง login ก่อนแล้วใช้ `POST /api/restaurants/join`
- ตอนนี้ยังไม่มี API `leave restaurant`, `remove member`, `delete restaurant`

## 8. API ที่ยังไม่มีในระบบตอนนี้

จากโค้ดปัจจุบัน ยังไม่พบ controller สำหรับหมวด:
- ingredients
- expiry items / stock
- usage history
- suggestions
- notifications
- OCR / scan

ดังนั้นถ้าฝั่ง frontend จะเริ่มต่อระบบตอนนี้ ควรโฟกัสที่ 3 ส่วนก่อน:
- authentication
- user profile
- restaurant setup / join / member list


-------------------------

 คุณคิดว่าแบบไหมเหมาะสมกว่ากัน และ ถ้าเป็น 1 owner + many managers + many employees flow การทำงานควรเป็นแบบใด เมื่อมีการ register เข้ามา
  แแล้วต้องการเป็น manager อีกคน ไม่ใช้สร้างร้าน


• ถ้ามองเชิงระบบใช้งานจริง ผมคิดว่า 1 owner + many managers + many employees เหมาะกว่า แต่ไม่ควรใช้ model ปัจจุบันต่อโดยตรง เพราะของเดิมเป็น global
  role + restaurantId เดียว ซึ่งไม่พอสำหรับสิทธิ์หลายระดับในร้าน

  แบบที่เหมาะสุดในทางปฏิบัติคือ:

  - Owner มี 1 คนต่อร้าน
  - Manager มีได้หลายคน
  - Employee มีได้หลายคน

  เหตุผล:

  - ร้านจริงมักมีผู้จัดการหลายคนได้ เช่น ผู้ช่วยผู้จัดการ, หัวหน้ากะ
  - แต่ควรมี owner คนเดียวสำหรับเรื่องสำคัญ เช่น เปลี่ยน owner, ลบร้าน, รีเซ็ต invite code, จัดการ manager คนอื่น
  - ถ้าให้ทุก manager เท่ากันหมด จะเริ่มควบคุมสิทธิ์ยาก

  Flow ที่แนะนำถ้ามี many managers

  ตอน register:

  - ทุกคนสมัคร account ก่อน
  - ตอนสมัครยังไม่ต้องเป็น manager ของร้านไหน
  - อาจมีแค่ role = EMPLOYEE เป็น default ทั้งหมด หรือมี platform role ที่ไม่ผูกกับร้าน
  - หลัง login ค่อย “เข้าร้าน” ผ่าน invite/join flow

  ผมแนะนำว่าอย่าให้ “register แล้วเลือกเป็น manager ของร้านอื่นได้เลย” เพราะจะเปิดช่องสิทธิ์ผิดง่ายเกินไป ควรให้ owner ของร้านเป็นคนแต่งตั้งภายหลัง

  Flow ที่ถูกกว่า

  1. ผู้ใช้สมัครสมาชิก
  2. login
  3. ถ้ายังไม่มีร้าน:
      - ถ้าต้องการสร้างร้านเอง -> เข้า flow สร้างร้าน และคนนั้นกลายเป็น Owner
      - ถ้าต้องการเข้าร้านที่มีอยู่ -> ใช้ invite code join เป็น Employee ก่อน

  4. หลังเข้าร้านแล้ว ถ้า owner ต้องการให้คนนี้เป็น manager:
      - owner กด promote จาก employee -> manager

  ดังนั้นกรณี “register เข้ามาแล้วอยากเป็น manager อีกคน แต่ไม่ได้สร้างร้าน” flow ควรเป็น:

  1. สมัคร account
  2. login
  3. join ร้านด้วย invite code
  4. owner ของร้าน promote บัญชีนั้นเป็น Manager

  สิทธิ์ที่ควรแยก

  - Owner
      - แก้ข้อมูลร้าน
      - regenerate invite code
      - ดูสมาชิก
      - promote/demote manager
      - transfer ownership
      - ลบร้าน

      - ดูสมาชิก
      - จัดการ employee
      - อาจใช้ invite code ได้
      - แต่ไม่ควร promote manager คนอื่นหรือเปลี่ยน owner

  - Employee
      - ใช้งานปกติในร้าน
      - แก้โปรไฟล์ตัวเอง
      - ไม่มีสิทธิ์ตั้งค่าร้าน

  โครงสร้างข้อมูลที่ควรเปลี่ยน
  ตอนนี้ไม่ควรเก็บแค่:

  - user.role
  - user.restaurantId

  ควรเปลี่ยนเป็น membership model เช่น:

  - restaurants
  - users
  - restaurant_members
      - user_id
      - restaurant_id
      - restaurant_role = OWNER | MANAGER | EMPLOYEE

  แบบนี้ถึงจะรองรับหลาย manager ได้สะอาด

  สรุป
  ถ้าจะโตต่อจริง ควรไปทาง 1 owner + many managers + many employees
  และ flow ที่เหมาะคือ:

  - สมัคร account ก่อน
  - join ร้านก่อน
  - owner ค่อย promote เป็น manager
  - คนที่สร้างร้านเป็น owner อัตโนมัติ

  ถ้าต้องการ ผมช่วยต่อได้ 2 อย่าง:

  1. ออกแบบ database/schema ใหม่สำหรับ restaurant_members
  2. วาง API flow ใหม่ทั้งหมดสำหรับ owner-manager-employee ก่อนลงมือแก้โค้ด