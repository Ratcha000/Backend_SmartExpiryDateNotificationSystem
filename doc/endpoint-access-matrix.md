# ตาราง Endpoint และสิทธิ์การเข้าถึง

อ้างอิงจากโค้ดในโปรเจกต์ `expiry-system` ณ ปัจจุบัน โดยดูจาก `SecurityConfig`, controller และ service logic จริง

## คำอธิบายสิทธิ์

- `Public` = เรียกได้โดยไม่ต้องมี Access Token
- `Manager` = ผู้ใช้ role `MANAGER`
- `Employee` = ผู้ใช้ role `EMPLOYEE`
- ถ้ามีเงื่อนไขเพิ่ม เช่น ต้องเป็นคนในร้านเดียวกัน หรือทำได้เฉพาะบางกรณี จะระบุในคอลัมน์ `หน้าที่`

## 1. Authentication

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | ✓ | ✓ | ✓ | redirect ไปหน้า Swagger UI |
| POST | `/api/auth/register` | ✓ | ✓ | ✓ | สมัครสมาชิกใหม่ รองรับทั้ง manager และ employee |
| POST | `/api/auth/login` | ✓ | ✓ | ✓ | เข้าสู่ระบบและรับ access token/refresh token |
| POST | `/api/auth/refresh` | ✓ | ✓ | ✓ | ต่ออายุ access token ด้วย refresh token |
| POST | `/api/auth/logout` | ✗ | ✓ | ✓ | ออกจากระบบฝั่ง client โดยให้ client ทิ้ง token ที่เก็บไว้ |
| GET | `/api/auth/me` | ✗ | ✓ | ✓ | ดูข้อมูลผู้ใช้ที่ login อยู่ |

## 2. Users

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/users/me` | ✗ | ✓ | ✓ | ดูข้อมูลบัญชีของตัวเอง |
| GET | `/api/users/{id}` | ✗ | ✓ | ✓ | ดูข้อมูลผู้ใช้ตาม id |
| PUT | `/api/users/{id}` | ✗ | ✓ | ✓ | แก้ไขข้อมูลผู้ใช้ โดย employee แก้ได้เฉพาะตัวเอง ส่วน manager แก้ผู้ใช้ในร้านเดียวกันได้ |
| GET | `/api/users` | ✗ | ✓ | ✓ | ดูรายชื่อผู้ใช้ทั้งหมด หรือกรองด้วย `restaurantId` |

## 3. Restaurants

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/restaurants` | ✗ | ✓ | ✗ | สร้างร้านอาหารใหม่ เฉพาะ manager ที่ยังไม่สังกัดร้าน |
| GET | `/api/restaurants/{id}` | ✗ | ✓ | ✓ | ดูข้อมูลร้านตาม id โดยต้องเป็นสมาชิกของร้านนั้น |
| GET | `/api/restaurants/me` | ✗ | ✓ | ✓ | ดูข้อมูลร้านของผู้ใช้ปัจจุบัน |
| PUT | `/api/restaurants/{id}` | ✗ | ✓ | ✗ | แก้ไขข้อมูลร้าน เฉพาะ manager เจ้าของร้าน |
| GET | `/api/restaurants/invite/{inviteCode}` | ✗ | ✓ | ✓ | ดูข้อมูลร้านจาก invite code |
| POST | `/api/restaurants/join` | ✗ | ✗ | ✓ | เข้าร่วมร้านด้วย invite code เฉพาะ employee ที่ยังไม่สังกัดร้าน |
| POST | `/api/restaurants/{id}/invite-code` | ✗ | ✓ | ✗ | สร้าง invite code ใหม่ เฉพาะ manager เจ้าของร้าน |
| GET | `/api/restaurants/{id}/members` | ✗ | ✓ | ✓ | ดูสมาชิกในร้าน โดยต้องเป็นสมาชิกของร้านนั้น |

## 4. Ingredients

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/ingredients` | ✗ | ✓ | ✓ | เพิ่มวัตถุดิบใหม่ในร้านของตัวเอง |
| GET | `/api/ingredients` | ✗ | ✓ | ✓ | ดูรายการวัตถุดิบ โดยส่ง `restaurantId` และ filter เพิ่มได้ |
| GET | `/api/ingredients/{id}` | ✗ | ✓ | ✓ | ดูรายละเอียดวัตถุดิบตาม id |
| PUT | `/api/ingredients/{id}` | ✗ | ✓ | ✓ | แก้ไขข้อมูลวัตถุดิบ |
| PATCH | `/api/ingredients/{id}/consume` | ✗ | ✓ | ✓ | ตัดจำนวนวัตถุดิบจากการใช้งาน |
| PATCH | `/api/ingredients/{id}/restock` | ✗ | ✓ | ✓ | เติมสต็อกวัตถุดิบ |
| PATCH | `/api/ingredients/{id}/adjust-quantity` | ✗ | ✓ | ✗ | ปรับจำนวนคงเหลือโดยตรง เฉพาะ manager |
| PATCH | `/api/ingredients/{id}/used` | ✗ | ✓ | ✓ | ทำเครื่องหมายว่าวัตถุดิบถูกใช้หมดแล้ว |
| PATCH | `/api/ingredients/{id}/delete` | ✗ | ✓ | ✓ | ลบแบบ soft delete |
| PATCH | `/api/ingredients/{id}/status` | ✗ | ✓ | ✓ | เปลี่ยนสถานะวัตถุดิบ |
| GET | `/api/ingredients/expiring` | ✗ | ✓ | ✓ | ดูวัตถุดิบที่ใกล้หมดอายุในร้าน |
| GET | `/api/ingredients/expired` | ✗ | ✓ | ✓ | ดูวัตถุดิบที่หมดอายุในร้าน |
| GET | `/api/ingredients/low-stock` | ✗ | ✓ | ✓ | ดูวัตถุดิบที่สต็อกต่ำในร้าน |

## 5. Usage History

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/usage-history` | ✗ | ✓ | ✓ | สร้างประวัติการใช้งานวัตถุดิบ |
| GET | `/api/usage-history` | ✗ | ✓ | ✗ | ดูรายการประวัติการใช้งานของร้าน เฉพาะ manager |
| GET | `/api/usage-history/{id}` | ✗ | ✓ | ✗ | ดูรายละเอียดประวัติการใช้งานตาม id เฉพาะ manager |

## 6. เอกสารระบบ

| Method | Endpoint | Public | Manager | Employee | หน้าที่ |
| --- | --- | --- | --- | --- | --- |
| GET | `/swagger-ui.html` | ✓ | ✓ | ✓ | หน้า Swagger UI |
| GET | `/swagger-ui/**` | ✓ | ✓ | ✓ | static asset ของ Swagger UI |
| GET | `/v3/api-docs/**` | ✓ | ✓ | ✓ | OpenAPI schema |

## หมายเหตุสำคัญจากโค้ดจริง

- หลาย endpoint ในหมวด `Ingredients`, `Restaurants` และ `Usage History` แม้ role จะผ่าน แต่ยังต้องเป็นผู้ใช้ที่อยู่ใน `restaurantId` เดียวกันด้วย
- `GET /api/users` และ `GET /api/users/{id}` ตอนนี้เปิดให้ผู้ใช้ที่ login แล้วทุก role เรียกได้ เพราะไม่มีการเช็ก role เพิ่มใน service
- `PUT /api/users/{id}` มีพฤติกรรมพิเศษ: ถ้าเป็น manager สามารถแก้ข้อมูลผู้ใช้ในร้านเดียวกันได้ แต่การเปลี่ยน `role` ในโค้ดปัจจุบันจะเกิดขึ้นได้เฉพาะกรณีแก้บัญชีของตัวเอง
